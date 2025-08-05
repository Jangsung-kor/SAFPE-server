package com.example.SAFPE.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.SAFPE.config.FileStorageConfig;

@Service
public class FileStorageService {
	private final Path fileStorageLocation;

	public FileStorageService(FileStorageConfig fileStorageConfig) {
		fileStorageLocation = Paths.get(fileStorageConfig.getUploadDir()).toAbsolutePath().normalize();

		try {
			Files.createDirectories(fileStorageLocation);
		} catch (Exception ex) {
			throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
		}
	}

	public String storeFile(MultipartFile file) throws IOException {
		// 파일 이름의 비정상적인 문자열을 정리
		String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
		// 동일한 이름의 파일이 업로드되는 것을 방지하기 위해 UUID 추가
		String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

		// 대상 경로 설정
		Path targetLocation = fileStorageLocation.resolve(fileName);
		// 파일을 대상 경로에 복사 (같은 이름이 있으면 덮어쓰기)
		Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

		return fileName;
	}
}
