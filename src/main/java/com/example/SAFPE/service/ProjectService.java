package com.example.SAFPE.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.SAFPE.dto.CreateProjectRequest;
import com.example.SAFPE.dto.DoorDto;
import com.example.SAFPE.dto.MetricsDto;
import com.example.SAFPE.dto.PlanDataDto;
import com.example.SAFPE.dto.PointDto;
import com.example.SAFPE.dto.ProjectDto;
import com.example.SAFPE.dto.UpdateProjectRequest;
import com.example.SAFPE.dto.WallDto;
import com.example.SAFPE.dto.WindowDto;
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

	/**
	 * 프로젝트를 지정된 포맷(png, pdf) 파일로 내보낸다.
	 * 
	 * @param projectId
	 * @param format    'png' 또는 'pdf
	 * @return
	 * @throws IOException
	 */
	public byte[] exportProject(Long projectId, String format) throws IOException {
		ProjectDto projectDto = getProjectById(projectId);
		PlanDataDto planData = projectDto.getPlanData();

		if ("png".equalsIgnoreCase(format)) {
			return createImageFromPlan(planData, projectDto.getTitle());
		} else if ("pdf".equalsIgnoreCase(format)) {
			return createPdfFromPlanWithPdfBox(planData, projectDto.getTitle());
		} else {
			throw new IllegalArgumentException("Unsupported format: " + format);
		}
	}

	// 평면도 데이터로 PNG 이미지를 생성한다.
	private byte[] createImageFromPlan(PlanDataDto planData, String title) throws IOException {
		int width = 1200; // 이미지 가로 크기
		int height = 800; // 이미지 세로 크기
		int padding = 50; // 여백

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();

		// 흰색 배경
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, width, height);

		// 제목
		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
		g2d.drawString(title, padding, padding);

		// 렌더링 품질 향상
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// 여백을 고려해서 좌표 이동
		g2d.translate(padding, padding + 20);

		if (planData != null) {
			// 벽 그리기
			g2d.setStroke(new BasicStroke(5));
			g2d.setColor(new Color(88, 101, 242)); // #5865f2
			if (planData.getWalls() != null) {
				for (WallDto wall : planData.getWalls()) {
					g2d.drawLine((int) wall.getStart().getX(), (int) wall.getStart().getY(), (int) wall.getEnd().getX(),
							(int) wall.getEnd().getY());
				}
			}

			// 문 그리기
			g2d.setColor(new Color(242, 163, 88)); // #f2a358
			if (planData.getDoors() != null) {
				for (DoorDto door : planData.getDoors()) {
					g2d.fillRect((int) (door.getPosition().getX() - door.getWidth() / 2),
							(int) (door.getPosition()).getY() - 10, (int) door.getWidth(), 20);
				}
			}

			// 창문 그리기
			g2d.setColor(new Color(88, 201, 242)); // #58c9f2
			if (planData.getWindows() != null) {
				for (WindowDto window : planData.getWindows()) {
					g2d.fillRect((int) (window.getPosition().getX() - window.getWidth() / 2),
							(int) window.getPosition().getY() - 5, (int) window.getWidth(), 10);
				}
			}
		}

		g2d.dispose();

		// 이미지를 byte 배열로 변환
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		return baos.toByteArray();
	}

	// 평면도 데이터를 PDF 문서로 생성한다.
	private byte[] createPdfFromPlanWithPdfBox(PlanDataDto planData, String title) throws IOException {
		// try-with-resources 구문으로 PDDocumeent가 자동으로 닫히도록 함
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);

			float yOffset = page.getMediaBox().getHeight() - 70; // PDF는 좌츨 하단이 (0,0)이므로 y좌표 변환을 위한 오프셋

			// PDPageContentStream도 자동으로 닫힘
			try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
				// 제목 추가 -> 한글 폰트 지원 안되는 문제로 제목 지원 안함
//				contentStream.beginText();
//				contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 24);
//				contentStream.newLineAtOffset(50, yOffset + 20);
//				contentStream.showText(title);
//				contentStream.endText();

				if (planData != null) {
					// 벽 그리기
					contentStream.setStrokingColor(new Color(88, 101, 242)); // #5865f2
					contentStream.setLineWidth(5);
					if (planData.getWalls() != null) {
						for (WallDto wall : planData.getWalls()) {
							contentStream.moveTo((float) wall.getStart().getX() + 50,
									yOffset - (float) wall.getStart().getY());
							contentStream.lineTo((float) wall.getEnd().getX() + 50,
									yOffset - (float) wall.getEnd().getY());
							contentStream.stroke();
						}
					}

					// 문 그리기 (PDFBox에서는 setNonStrokingColor로 채우기 색상 지정해야함)
					contentStream.setNonStrokingColor(new Color(242, 163, 88)); // #f2a358
					if (planData.getDoors() != null) {
						for (DoorDto door : planData.getDoors()) {
							contentStream.addRect((float) (door.getPosition().getX() - door.getWidth() / 2) + 50,
									yOffset - (float) door.getPosition().getY() - 10, (float) door.getWidth(), 20);
							contentStream.fill();
						}
					}

					// 창문 그리기
					contentStream.setNonStrokingColor(new Color(88, 201, 242)); // #58c9f2
					if (planData.getWindows() != null) {
						for (WindowDto window : planData.getWindows()) {
							contentStream.addRect((float) (window.getPosition().getX() - window.getWidth() / 2) + 50,
									yOffset - (float) window.getPosition().getY() - 5, (float) window.getWidth(), 10);
							contentStream.fill();
						}
					}
				}
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			document.save(baos);
			return baos.toByteArray();
		}
	}
}
