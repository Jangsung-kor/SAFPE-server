package com.example.SAFPE.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.SAFPE.dto.CreateProjectRequest;
import com.example.SAFPE.dto.MetricsDto;
import com.example.SAFPE.dto.PlanDataDto;
import com.example.SAFPE.dto.PointDto;
import com.example.SAFPE.dto.ProjectDto;
import com.example.SAFPE.dto.UpdateProjectRequest;
import com.example.SAFPE.dto.WallDto;
import com.example.SAFPE.entity.Project;
import com.example.SAFPE.exception.ResourceNotFoundException;
import com.example.SAFPE.repository.ProjectRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * 비즈니스 로직
 */

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 주입
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final FileStorageService fileStorageService;
	private final ObjectMapper objectMapper; // JSON 변환을 위해 주입

	// Project 엔티티를 ProjectDto로 변환
	private ProjectDto convertToDto(Project project) {
		PlanDataDto planDataDto = null;
		if (project.getPlanData() != null && !project.getPlanData().isEmpty()) {
			try {
				planDataDto = objectMapper.readValue(project.getPlanData(), PlanDataDto.class);
			} catch (JsonProcessingException e) {
				// 로그를 남기거나 예외 처리
				e.printStackTrace();
			}
		}

		// 계산된 메트릭스 추가
		MetricsDto metrics = calculateMetrics(planDataDto);

		return ProjectDto.builder().id(project.getId()).title(project.getTitle())
				.backgroundImageUrl(project.getBackgroundImageUrl()).planData(planDataDto).metrics(metrics)
				.createdAt(project.getCreateAt()).updateAt(project.getUpdateAt()).build();
	}

	// 길이와 면적 계산 로직
	private MetricsDto calculateMetrics(PlanDataDto planData) {
		if (planData == null || planData.getWalls() == null || planData.getWalls().isEmpty()) {
			return MetricsDto.builder().totalWallLength(0).estimatedArea(0).unit("pixel").build();
		}

		// 1. 총 벽 길이 계산
		double totalLength = 0;
		for (WallDto wall : planData.getWalls()) {
			double length = Math.sqrt(Math.pow(wall.getEnd().getX() - wall.getStart().getX(), 2)
					+ Math.pow(wall.getEnd().getY() - wall.getStart().getY(), 2));
			totalLength += length;
		}

		// 2. 면적 계산 (Shoelace Formula 사용)
		// 벽의 시작점들을 다각형의 꼭짓점으로 간주
		double area = 0;
		List<PointDto> vertices = planData.getWalls().stream().map(WallDto::getStart).collect(Collectors.toList());
		int n = vertices.size();

		if (n > 2) { // 최소 3개의 꼭짓점이 있어야 면적 계산 가능하니까
			for (int i = 0; i < n; i++) {
				PointDto current = vertices.get(i);
				PointDto next = vertices.get((i + 1) % n); // 마지막 꼭짓점은 첫 번째와연결
				area += (current.getX() * next.getY()) - (next.getX() * current.getY());
			}
			area = Math.abs(area) / 2.0;
		}

		return MetricsDto.builder().totalWallLength(Math.round(totalLength * 10) / 10.0) // 소수점 한 자리까지
				.estimatedArea(Math.round(area * 10) / 10.0).unit("pixel").build();
	}

	// 프로젝트 목록 조회
	public List<ProjectDto> getAllProjects() {
		return projectRepository.findAllByOrderByIdDesc().stream().map(this::convertToDto).collect(Collectors.toList());
	}

	// 프로젝트 상세 조회
	public ProjectDto getProjectById(Long id) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("project not found with id: " + id));

		return convertToDto(project);
	}

	// 새 프로젝트 생성
	@Transactional
	public ProjectDto createProject(CreateProjectRequest request) {
		// 초기 빈 데이터
		String planData = "{\"walls\": [], \"doors\": [], \"windows\": []}";
		Project project = Project.builder().title(request.getTitle()).planData(planData).build();

		Project savedProject = projectRepository.save(project);
		return convertToDto(savedProject);
	}

	// 프로젝트 정보와 평면도 데이터 업데이트
	@Transactional
	public ProjectDto updateProject(Long id, UpdateProjectRequest request) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found with id" + id));

		project.setTitle(request.getTitle());

		try {
			String planDataJson = objectMapper.writeValueAsString(request.getPlanData());
			project.setPlanData(planDataJson);
		} catch (JsonProcessingException e) {
			// 적절한 예외 처리
			throw new RuntimeException("Failed to seralize plan data", e);
		}

		Project savedProject = projectRepository.save(project);
		return convertToDto(savedProject);
	}

	// 배경 이미지 업로드
	@Transactional
	public ProjectDto uploadBackgroundImage(Long id, MultipartFile file) throws IOException {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

		String fileName = fileStorageService.storeFile(file);
		// 웹에서 접근 가능한 경로로 만들어 저장
		String fileDownloadUrl = "/uploads/" + fileName;

		project.setBackgroundImageUrl(fileDownloadUrl);

		return convertToDto(project);
	}
}
