package com.example.SAFPE.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable // 이 클래스가 다른 엔티티에 포함될 수 있음을 나타냄
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Point {
	private double x;
	private double y;
}
