package com.example.SAFPE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.SAFPE.entity.Project;
import com.example.SAFPE.entity.User;

/**
 * JpaRepository를 상속받아 기본적인 CRUD 기능 자동 구현
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

	// 필요 시 커스텀 쿼리 메소드 추가 (예: findByUserId)
	List<Project> findAllByOrderByIdDesc(); // 최신순으로 정렬

	List<Project> findByUserOrderByIdDesc(User user);

	// 상세 정보 조회
	Optional<Project> findByUserAndId(User user, Long id);

	Optional<Project> findByShareIdAndIsPublicTrue(String shareId);
}
