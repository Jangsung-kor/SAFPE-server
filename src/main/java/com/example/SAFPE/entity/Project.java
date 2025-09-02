package com.example.SAFPE.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

	/*
	 * @Lob // Large Object: 긴 텍스트 데이터를 저장하기 위함
	 * 
	 * @Column(columnDefinition = "TEXT") private String planData; // 프론트엔드의
	 * PlanData 객체를 JSON 문자열로 저장
	 */

	/*
	 * cascade = CascadeType.ALL : project 테이블의 데이터가 삭제,수정되면 벽/문/창문도 함께 삭제,수정된다.
	 * orphanRemoval = true : project에서 walls 리스트에서 특정 벽을 제거하면, DB에서도 해당 벽 레코드를
	 * 삭제한다.(편리한 기능)
	 */
	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Wall> walls = new ArrayList<>();

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Door> doors = new ArrayList<>();

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Window> windows = new ArrayList<>();

	@Column
	private Double scaleRatio;

	@Column(length = 10)
	private String scaleUnit;

	@CreationTimestamp
	private LocalDateTime createAt;

	@UpdateTimestamp
	private LocalDateTime updateAt;

	@ManyToOne(fetch = FetchType.LAZY) // 지연 로딩으로 성능 최적화
	@JoinColumn(name = "user_id") // 외래키 컬럼명 지정
	private User user;

	@Column(nullable = false)
	private boolean isPublic = false; // 기본값은 비공개

	@Column(unique = true)
	private String shareId; // 공유를 위한 고유 ID
}
