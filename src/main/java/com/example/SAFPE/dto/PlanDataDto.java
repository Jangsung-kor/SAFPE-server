package com.example.SAFPE.dto;

import java.util.List;

import lombok.Data;

@Data
public class PlanDataDto {
	private List<WallDto> walls;
	// TODO: DoorDto, WindowDto도 추가
}
