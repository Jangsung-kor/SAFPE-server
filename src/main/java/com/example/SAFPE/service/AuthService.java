package com.example.SAFPE.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.SAFPE.config.jwt.JwtTokenProvider;
import com.example.SAFPE.dto.AuthRequest;
import com.example.SAFPE.dto.AuthResponse;
import com.example.SAFPE.dto.RegisterRequest;
import com.example.SAFPE.entity.User;
import com.example.SAFPE.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	// Spring Boot 2.7.x 이상부터는 AuthenticationManager Bean이 자동으로 등록되지 않으므로 개발자가 직접 의존성 주입해야 함. 
	private final AuthenticationManager authenticationManager;
	
	// 회원가입
	public void register(RegisterRequest request) {
		// 사용자 이름 중복 확인
		if (userRepository.findByUsername(request.getUsername()).isPresent()) {
			throw new IllegalArgumentException("Username is already taken");
		}
		
		// 사용자 생성과 비밀번호 암호화
		User user = User.builder().username(request.getUsername())
				.password(passwordEncoder.encode(request.getPassword())).build();
		
		// 사용자 정보 저장
		userRepository.save(user);
	}
	
	// 로그인
	public AuthResponse login(AuthRequest request) {
		// Spring Security를 사용해서 사용자 인증
		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
		// 인증 정보를 SecurityContext에 저장
		SecurityContextHolder.getContext().setAuthentication(authentication);
		
		// JWT 토큰 생성
		String token = jwtTokenProvider.createToken(request.getUsername());
		
		// 토큰을 포함한 응답 반환
		return new AuthResponse(token);
	}
}
