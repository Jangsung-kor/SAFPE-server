package com.example.SAFPE.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.example.SAFPE.dto.CreateProjectRequest;
import com.example.SAFPE.dto.DoorDto;
import com.example.SAFPE.dto.MetricsDto;
import com.example.SAFPE.dto.PlanDataDto;
import com.example.SAFPE.dto.PointDto;
import com.example.SAFPE.dto.ProjectDto;
import com.example.SAFPE.dto.ScaleDto;
import com.example.SAFPE.dto.UpdateProjectRequest;
import com.example.SAFPE.dto.WallDto;
import com.example.SAFPE.dto.WindowDto;
import com.example.SAFPE.dto.ai.AiAnalysisResponse;
import com.example.SAFPE.entity.Door;
import com.example.SAFPE.entity.Point;
import com.example.SAFPE.entity.Project;
import com.example.SAFPE.entity.User;
import com.example.SAFPE.entity.Wall;
import com.example.SAFPE.entity.Window;
import com.example.SAFPE.exception.ResourceNotFoundException;
import com.example.SAFPE.repository.ProjectRepository;
import com.example.SAFPE.repository.UserRepository;
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
	private final UserRepository userRepository;

	// Python 서버와 통신하기 위한 RestTemplate
	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${ai.server.url}") // application.yml에서 AI 서버 주소 주입
	private String aiServerUrl;

	private final ObjectMapper objectMapper;

	// 현재 로그인된 사용자를 가져오는 Helper 메소드
	private User getCurrentUser() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	// Project 엔티티를 ProjectDto로 변환
	private ProjectDto convertToDto(Project project) {
		// 엔티티 리스트를 DTO 리스트로 변환
		List<WallDto> wallDtos = new ArrayList<>();
		if (project.getWalls() != null && !project.getWalls().isEmpty()) {
			wallDtos = project.getWalls().stream().map(wall -> {
				WallDto dto = new WallDto();
				dto.setStart(new PointDto(wall.getStartPoint().getX(), wall.getStartPoint().getY()));
				dto.setEnd(new PointDto(wall.getEndPoint().getX(), wall.getEndPoint().getY()));
				return dto;
			}).collect(Collectors.toList());
		}

		List<DoorDto> doorDtos = new ArrayList<>();
		if (project.getDoors() != null && !project.getDoors().isEmpty()) {
			doorDtos = project.getDoors().stream().map(door -> {
				DoorDto dto = new DoorDto();
				dto.setPosition(new PointDto(door.getPosition().getX(), door.getPosition().getY()));
				dto.setWidth(door.getWidth());
				return dto;
			}).collect(Collectors.toList());
		}

		List<WindowDto> windowDtos = new ArrayList<>();
		if (project.getWindows() != null && !project.getWindows().isEmpty()) {
			windowDtos = project.getWindows().stream().map(window -> {
				WindowDto dto = new WindowDto();
				dto.setPosition(new PointDto(window.getPosition().getX(), window.getPosition().getY()));
				dto.setWidth(window.getWidth());
				return dto;
			}).collect(Collectors.toList());
		}

		PlanDataDto planDataDto = new PlanDataDto();
		planDataDto.setWalls(wallDtos);
		planDataDto.setDoors(doorDtos);
		planDataDto.setWindows(windowDtos);

		// 계산된 메트릭스 추가
		MetricsDto metrics = calculateMetrics(planDataDto, project.getScaleRatio(), project.getScaleUnit());

		return ProjectDto.builder().id(project.getId()).title(project.getTitle())
				.backgroundImageUrl(project.getBackgroundImageUrl()).planData(planDataDto)
				.scaleRatio(project.getScaleRatio()).scaleUnit(project.getScaleUnit()).metrics(metrics)
				.createdAt(project.getCreateAt()).updateAt(project.getUpdateAt()).isPublic(project.isPublic())
				.shareId(project.getShareId()).build();
	}

	// 길이와 면적 계산 로직
	private MetricsDto calculateMetrics(PlanDataDto planData, Double scaleRatio, String scaleUnit) {
		if (planData == null || planData.getWalls() == null || planData.getWalls().isEmpty()) {
			return MetricsDto.builder().totalWallLength(0).estimatedArea(0).unit("pixel").build();
		}

		// 1. 총 벽 길이 계산
		double totalPixelLength = 0;
		for (WallDto wall : planData.getWalls()) {
			double length = Math.sqrt(Math.pow(wall.getEnd().getX() - wall.getStart().getX(), 2)
					+ Math.pow(wall.getEnd().getY() - wall.getStart().getY(), 2));
			totalPixelLength += length;
		}

		// 2. 면적 계산 (Shoelace Formula 사용)
		// 벽의 시작점들을 다각형의 꼭짓점으로 간주
		double pixelArea = 0;
		List<PointDto> vertices = planData.getWalls().stream().map(WallDto::getStart).collect(Collectors.toList());
		int n = vertices.size();

		if (n > 2) { // 최소 3개의 꼭짓점이 있어야 면적 계산 가능하니까
			for (int i = 0; i < n; i++) {
				PointDto current = vertices.get(i);
				PointDto next = vertices.get((i + 1) % n); // 마지막 꼭짓점은 첫 번째와연결
				pixelArea += (current.getX() * next.getY()) - (next.getX() * current.getY());
			}
			pixelArea = Math.abs(pixelArea) / 2.0;
		}

		// 스케일 적용
		double finalLength = totalPixelLength;
		double finalArea = pixelArea;
		String finalUnit = "px";

		if (scaleRatio != null && scaleRatio > 0 && scaleUnit != null) {
			finalLength = totalPixelLength * scaleRatio;
			finalArea = pixelArea * Math.pow(scaleRatio, 2);
			finalUnit = scaleUnit;
		}

		return MetricsDto.builder().totalWallLength(Math.round(finalLength * 100) / 100.0) // 소수점 두 자리까지
				.estimatedArea(Math.round(finalArea * 10) / 10.0).unit(finalUnit).build();
	}

	// 프로젝트 목록 조회
	public List<ProjectDto> getAllProjects() {
		User currentUser = getCurrentUser();

		return projectRepository.findByUserOrderByIdDesc(currentUser).stream().map(this::convertToDto)
				.collect(Collectors.toList());
	}

	// 프로젝트 상세 조회
	public ProjectDto getProjectById(Long id) {
		User currentUser = getCurrentUser();

		Project project = projectRepository.findByUserAndId(currentUser, id)
				.orElseThrow(() -> new ResourceNotFoundException("project not found with id: " + id));

		return convertToDto(project);
	}

	// 새 프로젝트 생성
	@Deprecated
	@Transactional
	public ProjectDto createProject(CreateProjectRequest request) {
		User currentUser = getCurrentUser();

		// 초기 빈 데이터
		// String planData = "{\"walls\": [], \"doors\": [], \"windows\": []}";
		// Project project =
		// Project.builder().user(currentUser).title(request.getTitle()).planData(planData).build();
		Project project = Project.builder().user(currentUser).title(request.getTitle()).build();

		Project savedProject = projectRepository.save(project);
		return convertToDto(savedProject);
	}

	/**
	 * 새 프로젝트 생성과 동시에 이미지 업로드
	 * 
	 * @param title
	 * @param file
	 * @return
	 * @throws IOException
	 * @createAt 2025.10.28
	 */
	@Transactional
	public ProjectDto createProjectWithImage(String title, MultipartFile file) throws IOException {
		// -- AI 서버로 이미지 분석 요청
		AiAnalysisResponse aiResponse = null;

		// AI 서버로 이미지 분석 요청 보내기
		try {
			// 1. 요청 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);

			// 2. 요청 바디 구성
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", new ByteArrayResource(file.getBytes()) {
				@Override
				public String getFilename() {
					return file.getOriginalFilename();
				}
			});

			// 3. HTTP 요청 엔티티 생성
			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

			// 4. RestTemplate을 사용해서 Python 서버에 POST 요청
			ResponseEntity<String> response = restTemplate.postForEntity(aiServerUrl + "/analyze", requestEntity,
					String.class);

			// 5. 응답 출력
			aiResponse = objectMapper.readValue(response.getBody(), AiAnalysisResponse.class);

		} catch (Exception e) {
			System.err.println("Failed to connect to AI server: " + e.getMessage());
		}

		// 파일 저장
		String fileName = fileStorageService.storeFile(file);
		// 웹에서 접근 가능한 경로로 만들어 저장
		String fileDownloadUrl = "/uploads/" + fileName;

		// 새 프로젝트 생성
		User currentUser = this.getCurrentUser();
		Project project = Project.builder().title(title).user(currentUser).backgroundImageUrl(fileDownloadUrl).build();

		project.setBackgroundImageUrl(fileDownloadUrl);

		// AI 분석 결과를 초기 평면도 데이터로 설정
		if (aiResponse != null && aiResponse.getDetectedLines() != null) {
			aiResponse.getDetectedLines().forEach(wallDto -> {
				Wall wall = Wall.builder().startPoint(new Point(wallDto.getStart().getX(), wallDto.getStart().getY()))
						.endPoint(new Point(wallDto.getEnd().getX(), wallDto.getEnd().getY())).project(project).build();
				project.getWalls().add(wall);
			});
		}

		Project savedProject = projectRepository.save(project);
		return convertToDto(savedProject);
	}

	// 프로젝트 정보와 평면도 데이터 업데이트
	@Transactional
	public ProjectDto updateProject(Long id, UpdateProjectRequest request) {
		User currentUser = getCurrentUser();

		Project project = projectRepository.findByUserAndId(currentUser, id)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found with id" + id));

		// 1. 프로젝트 메타데이터 업데이트
		project.setTitle(request.getTitle());

		// 스케일 정보 업데이트
		if (request.getScale() != null && request.getScale().getPixelLength() > 0) {
			ScaleDto scale = request.getScale();
			project.setScaleRatio(scale.getRealLength() / scale.getPixelLength());
			project.setScaleUnit(scale.getUnit());
		}

		// 2. 기존 평면도 요소들 모두 삭제
		project.getWalls().clear();
		project.getDoors().clear();
		project.getWindows().clear();

		// 3. 요청받은 DTO 데이터로 새로운 엔티티 객체들을 생성하여 추가
		PlanDataDto planData = request.getPlanData();
		if (planData.getWalls() != null) {
			planData.getWalls().forEach(wallDto -> {
				Wall wall = Wall.builder().startPoint(new Point(wallDto.getStart().getX(), wallDto.getStart().getY()))
						.endPoint(new Point(wallDto.getEnd().getX(), wallDto.getEnd().getY())).project(project).build();
				project.getWalls().add(wall);
			});
		}
		if (planData.getDoors() != null) {
			planData.getDoors().forEach(doorDto -> {
				Door door = Door.builder()
						.position(new Point(doorDto.getPosition().getX(), doorDto.getPosition().getY()))
						.width(doorDto.getWidth()).project(project).build();
				project.getDoors().add(door);
			});
		}
		if (planData.getWindows() != null) {
			planData.getWindows().forEach(windowDto -> {
				Window window = Window.builder()
						.position(new Point(windowDto.getPosition().getX(), windowDto.getPosition().getY()))
						.width(windowDto.getWidth()).project(project).build();
				project.getWindows().add(window);
			});
		}

		Project savedProject = projectRepository.save(project);
		return convertToDto(savedProject);
	}

	// 배경 이미지 업로드
	@Transactional
	public ProjectDto uploadBackgroundImage(Long id, MultipartFile file) throws IOException {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

		// -- AI 서버로 이미지 분석 요청
		AiAnalysisResponse aiResponse = null;

		// AI 서버로 이미지 분석 요청 보내기
		try {
			// 1. 요청 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);

			// 2. 요청 바디 구성
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", new ByteArrayResource(file.getBytes()) {
				@Override
				public String getFilename() {
					return file.getOriginalFilename();
				}
			});

			// 3. HTTP 요청 엔티티 생성
			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

			// 4. RestTemplate을 사용해서 Python 서버에 POST 요청
			ResponseEntity<String> response = restTemplate.postForEntity(aiServerUrl + "/analyze", requestEntity,
					String.class);

			// 5. 응답 출력
			aiResponse = objectMapper.readValue(response.getBody(), AiAnalysisResponse.class);

		} catch (Exception e) {
			System.err.println("Failed to connect to AI server: " + e.getMessage());
		}

		// 파일 저장
		String fileName = fileStorageService.storeFile(file);
		// 웹에서 접근 가능한 경로로 만들어 저장
		String fileDownloadUrl = "/uploads/" + fileName;

		project.setBackgroundImageUrl(fileDownloadUrl);

		// AI 분석 결과를 초기 평면도 데이터로 설정
		if (aiResponse != null && aiResponse.getDetectedLines() != null) {
			aiResponse.getDetectedLines().forEach(wallDto -> {
				Wall wall = Wall.builder().startPoint(new Point(wallDto.getStart().getX(), wallDto.getStart().getY()))
						.endPoint(new Point(wallDto.getEnd().getX(), wallDto.getEnd().getY())).project(project).build();
				project.getWalls().add(wall);
			});
		}

		Project savedProject = projectRepository.save(project);
		return convertToDto(savedProject);
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

	/**
	 * 프로젝트 공유 설정 변경
	 * 
	 * @author Jangsung
	 * @param projectId
	 * @param isPublic
	 * @return
	 * @create_At 2025.08.24
	 */
	@Transactional
	public ProjectDto updateShareSettings(Long projectId, boolean isPublic) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found"));

		// ---중요: 자신의 프로젝트가 맞는지 권한 확인 로직
		User currentUser = getCurrentUser();
		if (!project.getUser().getId().equals(currentUser.getId())) {
			throw new IllegalStateException("You do not have permission to modify this project.");
		}

		project.setPublic(isPublic);

		if (isPublic && project.getShareId() == null) {
			// 프로젝트를 처음 공개할 때, 고유한 ID 생성
			project.setShareId(UUID.randomUUID().toString());
		}
		// 비공개로 전환해도 sharedId는 유지할 수 있다. (나중에 다시 공개할 때 같은 링크 사용 가능)
		// 만약 비공개 시 링크를 만료시키려면 project.setShareId(null);로 설정

		Project savedProject = projectRepository.save(project);
		return convertToDto(savedProject);
	}

	/**
	 * 공유 ID로 공개 프로젝트 조회 (인증 불필요)
	 * 
	 * @author Jangsungd
	 * @param shareId
	 * @return
	 * @create_At 2025.08.24
	 */
	public ProjectDto getPublicProjectByShareId(String shareId) {
		Project project = projectRepository.findByShareIdAndIsPublicTrue(shareId)
				.orElseThrow(() -> new ResourceNotFoundException("Public project not found or access denied"));
		return convertToDto(project);
	}
}
