package com.example.SAFPE.dto;

import lombok.Data;

@Data
public class UpdateProjectRequest {
	private String title;
	private PlanDataDto planData;
	private ScaleDto scale;
}
