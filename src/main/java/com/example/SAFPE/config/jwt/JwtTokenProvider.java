package com.example.SAFPE.config.jwt;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtTokenProvider {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.expiration}")
	private long expirationInMs;

	private Key key;

	@PostConstruct
	protected void init() {
//		key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
		// Decoders.BASE64대신 Decoders.BASE64URL 사용
		key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
	}

	public String createToken(String username) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expirationInMs);

		return Jwts.builder().setSubject(username).setIssuedAt(now).setExpiration(expiryDate)
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	public String getUsernameFromToken(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (Exception e) {
			// MalformedJwtExceptino, ExpiredJwtException 등
		}
		return false;
	}
}
