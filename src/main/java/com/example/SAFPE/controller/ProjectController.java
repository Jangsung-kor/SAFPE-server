package com.example.SAFPE.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
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

import com.example.SAFPE.dto.CreateProjectRequest;
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

	@PostMapping
	public ResponseEntity<ProjectDto> createProject(@RequestBody CreateProjectRequest request) {
		ProjectDto createdProject = projectService.createProject(request);
		return new ResponseEntity<>(createdProject, HttpStatus.CREATED);
	}

	@PutMapping("/{projectId}")
	public ResponseEntity<ProjectDto> updateProject(@PathVariable Long projectId,
			@RequestBody UpdateProjectRequest request) {
		return ResponseEntity.ok(projectService.updateProject(projectId, request));
	}

	@PostMapping("/{projectId}/background-image")
	public ResponseEntity<ProjectDto> uploadBackgroundImage(@PathVariable Long projectId,
			@RequestParam("file") MultipartFile file) throws IOException {
		return ResponseEntity.ok(projectService.uploadBackgroundImage(projectId, file));
	}
}
