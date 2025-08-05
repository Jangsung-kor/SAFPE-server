package com.example.SAFPE.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricsDto {
	private double totalWallLength;
	private double estimatedArea;
	private String unit;
}
