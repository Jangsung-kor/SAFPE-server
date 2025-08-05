package com.example.SAFPE.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.SAFPE.dto.ErrorResponse;

/**
 * @ResControllerAdvice = @ControllerAdvice + @ResponseBody
 * @ResControllerAdvice : REST API에서 JSON 형태로 객체를 반환할 때 사용
 * @ControllerAdvice : 모든 컨트롤러에서 발생하는 예외를 감지하고, @ExceptionHandler로 특정 예외를 지정하여
 *                   처리
 * @ResponseBody : HTTP 응답 상태 코드, 헤더, 본문을 직접 제어할 수 있는 객체
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 1. 우리가 직접 만든 ResourceNotFoundException 처리
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
		ErrorResponse response = ErrorResponse.builder().status(HttpStatus.NOT_FOUND.value())
				.error(HttpStatus.NOT_FOUND.getReasonPhrase()).message(ex.getMessage()).build();

		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	// 2. 유효하지 않은 인자(잘못된 요청)에 대한 처리
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handlerlllegalArgumentException(IllegalArgumentException ex) {
		ErrorResponse response = ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value())
				.error(HttpStatus.BAD_REQUEST.getReasonPhrase()).message(ex.getMessage()).build();

		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	// 3. 위에서 처리하지 못한 모든 예외 처리
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception ex) {
		ErrorResponse response = ErrorResponse.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.message("서버 내부 오류가 발생했습니다: " + ex.getMessage()).build();

		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
