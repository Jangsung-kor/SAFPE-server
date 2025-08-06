package com.example.SAFPE.dto;

import java.util.List;

import lombok.Data;

@Data
public class PlanDataDto {
	private List<WallDto> walls;
	private List<DoorDto> doors;
	private List<WindowDto> windows;
}
