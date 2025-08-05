package com.example.SAFPE.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * application.yml에 설정한 경로에 파일을 저정하고 관리하는 로직
 */

@Configuration
@ConfigurationProperties(prefix = "file")
@Getter
@Setter
public class FileStorageConfig {
	private String uploadDir;
}
