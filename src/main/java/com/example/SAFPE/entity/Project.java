package com.example.SAFPE.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 255)
	private String backgroundImageUrl;

	@Lob // Large Object: 긴 텍스트 데이터를 저장하기 위함
	@Column(columnDefinition = "TEXT")
	private String planData; // 프론트엔드의 PlanData 객체를 JSON 문자열로 저장

	@CreationTimestamp
	private LocalDateTime createAt;

	@UpdateTimestamp
	private LocalDateTime updateAt;
}
