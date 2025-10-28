package com.example.SAFPE.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.SAFPE.dto.ProjectDto;
import com.example.SAFPE.dto.UpdateProjectRequest;
import com.example.SAFPE.service.ProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
	private final ProjectService projectService;

	@GetMapping
	public ResponseEntity<List<ProjectDto>> getAllProjects() {
		return ResponseEntity.ok(projectService.getAllProjects());
	}

	@GetMapping("/{projectId}")
	public ResponseEntity<ProjectDto> getProejctById(@PathVariable("projectId") Long projectId) {
		return ResponseEntity.ok(projectService.getProjectById(projectId));
	}

//	@Deprecated
//	@PostMapping
//	public ResponseEntity<ProjectDto> createProject(@RequestBody CreateProjectRequest request) {
//		ProjectDto createdProject = projectService.createProject(request);
//		return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
//	}

	/**
	 * 프로젝트를 생성함게 동시에 이미지 업로드
	 * 
	 * @param title
	 * @param file
	 * @return
	 * @throws IOException
	 * @createAt 2025.10.28
	 */
	@PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<ProjectDto> createProjectWithImage(@RequestParam("title") String title,
			@RequestParam("file") MultipartFile file) throws IOException {
		ProjectDto createProject = projectService.createProjectWithImage(title, file);
		return new ResponseEntity<>(createProject, HttpStatus.CREATED);
	}

	@PutMapping("/{projectId}")
	public ResponseEntity<ProjectDto> updateProject(@PathVariable("projectId") Long projectId,
			@RequestBody UpdateProjectRequest request) {
		return ResponseEntity.ok(projectService.updateProject(projectId, request));
	}

	@PostMapping("/{projectId}/background-image")
	public ResponseEntity<ProjectDto> uploadBackgroundImage(@PathVariable("projectId") Long projectId,
			@RequestParam("file") MultipartFile file) throws IOException {
		return ResponseEntity.ok(projectService.uploadBackgroundImage(projectId, file));
	}

	@GetMapping("/{projectId}/export")
	public ResponseEntity<byte[]> exportProject(@PathVariable("projectId") Long projectId,
			@RequestParam(value = "format", defaultValue = "png") String format) {

		try {
			byte[] fileContent = projectService.exportProject(projectId, format);
			String fileName = "floorplan-" + projectId + "." + format.toLowerCase();

			MediaType mediaType;
			if ("pdf".equalsIgnoreCase(format)) {
				mediaType = MediaType.APPLICATION_PDF;
			} else if ("png".equalsIgnoreCase(format)) {
				mediaType = MediaType.IMAGE_PNG;
			} else {
				return ResponseEntity.badRequest().build();
			}

			return ResponseEntity.ok()
					// 헤더는 브라우저가 파일을 다운로드 하도록 함
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment: filename=\"" + fileName + "\"")
					.contentType(mediaType).body(fileContent);
		} catch (IOException e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage().getBytes());
		}
	}

	/**
	 * 공유 설정 변경
	 * 
	 * @author Jangsung
	 * @param projectId
	 * @param isPublic
	 * @return
	 * @create_At 2025.08.24
	 */
	@PutMapping("/{projectId}/share")
	public ResponseEntity<ProjectDto> updateShareSettings(@PathVariable("projectId") Long projectId,
			@RequestParam(value = "isPublic") boolean isPublic) {
		return ResponseEntity.ok(projectService.updateShareSettings(projectId, isPublic));
	}
}
