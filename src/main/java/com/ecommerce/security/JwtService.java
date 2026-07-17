package com.ecommerce.security;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	@Value("${app.jwt.secret}")
	private String secretBase64;

	@Value("${app.jwt.access-token-expiration-ms}")
	private long accessTokenExpirationMs;

	@Value("${app.jwt.refresh-token-expiration-ms}")
	private long refreshTokenExpirationMs;

	private SecretKey signingKey() {
		byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMs) {
		Date now = new Date();
		return Jwts.builder().claims(extraClaims).subject(userDetails.getUsername()).issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMs)).signWith(signingKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	public String generateAccessToken(UserDetails userDetails) {
		return buildToken(new HashMap<>(), userDetails, accessTokenExpirationMs);
	}

	public String generateRefreshToken(UserDetails userDetails) {
		return buildToken(new HashMap<>(), userDetails, refreshTokenExpirationMs);
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		Claims claims = Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
		return claimsResolver.apply(claims);
	}

	public String extractEmail(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	private boolean isTokenExpired(String token) {
		return extractClaim(token, Claims::getExpiration).before(new Date());
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String email = extractEmail(token);
		return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}
}
