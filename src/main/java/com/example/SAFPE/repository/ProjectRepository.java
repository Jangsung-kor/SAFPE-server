package com.example.SAFPE.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.SAFPE.entity.Project;

/**
 * JpaRepository를 상속받아 기본적인 CRUD 기능 자동 구현
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

	// 필요 시 커스텀 쿼리 메소드 추가 (예: findByUserId)
	List<Project> findAllByOrderByIdDesc(); // 최신순으로 정렬
}
