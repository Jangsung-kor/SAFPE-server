package com.example.SAFPE.dto.ai;

import java.util.List;

import com.example.SAFPE.dto.WallDto;

import lombok.Data;

@Data
public class AiAnalysisResponse {
	private String filename;
	private int width;
	private int height;
	private String format;
	private List<WallDto> detectedLines;
}
