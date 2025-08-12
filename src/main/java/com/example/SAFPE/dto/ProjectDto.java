package com.example.SAFPE.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 응답용 Dto
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
	private Long id;
	private String title;
	private String backgroundImageUrl;
	private PlanDataDto planData;
	private Double scaleRatio;
	private String scaleUnit;
	private MetricsDto metrics;
	private LocalDateTime createdAt;
	private LocalDateTime updateAt;
}
