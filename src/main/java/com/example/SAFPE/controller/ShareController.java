package com.example.SAFPE.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SAFPE.dto.ProjectDto;
import com.example.SAFPE.service.ProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {
	private final ProjectService projectService;

	/**
	 * 공유 링크로 프로젝트 조회
	 * 
	 * @author Jangsung
	 * @param shareId
	 * @return
	 * @create_At 2025.08.24
	 */
	@GetMapping("/{shareId}")
	public ResponseEntity<ProjectDto> getSharedProject(@PathVariable("shareId") String shareId) {
		return ResponseEntity.ok(projectService.getPublicProjectByShareId(shareId));
	}
}
