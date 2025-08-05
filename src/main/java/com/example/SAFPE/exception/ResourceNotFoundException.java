package com.example.SAFPE.exception;

/**
 * 예외가 발생하면 HTTP 상태 코드 404(NOT_FOUND)를 응답하도록 지정
 */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
