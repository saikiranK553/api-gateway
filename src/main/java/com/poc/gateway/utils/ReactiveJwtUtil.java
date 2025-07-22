package com.poc.gateway.utils;

import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import reactor.core.publisher.Mono;

@Component
public class ReactiveJwtUtil {
    
    @Value("${jwt.secret:YourSuperSecretKeyThatShouldBeLongEnough12345}")
    private String secretKey;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(new java.util.concurrent.Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    extractAllClaims(token);
                    
                    return !isTokenExpiredSync(token);
                    
                } catch (ExpiredJwtException | MalformedJwtException | UnsupportedJwtException | 
                         SignatureException | IllegalArgumentException e) {
                    return false;
                }
            }
        });
    }

    public Mono<Boolean> validateAccessToken(String token) {
        return Mono.fromCallable(new java.util.concurrent.Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    Claims claims = extractAllClaims(token);
                    String tokenType = claims.get("tokenType", String.class);
                    
                    return !isTokenExpiredSync(token) && "ACCESS".equals(tokenType);
                    
                } catch (ExpiredJwtException | MalformedJwtException | UnsupportedJwtException | 
                         SignatureException | IllegalArgumentException e) {
                    return false;
                }
            }
        });
    }

    public Mono<String> extractEmail(String token) {
        return Mono.fromCallable(new java.util.concurrent.Callable<String>() {
            @Override
            public String call() {
                try {
                    return extractClaim(token, Claims::getSubject);
                } catch (Exception e) {
                    throw new RuntimeException("Invalid token");
                }
            }
        });
    }

    public Mono<String> extractUserId(String token) {
        return Mono.fromCallable(new java.util.concurrent.Callable<String>() {
            @Override
            public String call() {
                try {
                    return extractClaim(token, claims -> claims.get("userId", String.class));
                } catch (Exception e) {
                    throw new RuntimeException("Invalid token");
                }
            }
        });
    }

    public Mono<String> extractRole(String token) {
        return Mono.fromCallable(new java.util.concurrent.Callable<String>() {
            @Override
            public String call() {
                try {
                    return extractClaim(token, claims -> claims.get("role", String.class));
                } catch (Exception e) {
                    throw new RuntimeException("Invalid token");
                }
            }
        });
    }

    public Mono<Boolean> isTokenExpired(String token) {
        return Mono.fromCallable(new java.util.concurrent.Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    Date expiration = extractExpiration(token);
                    return expiration.before(new Date());
                } catch (Exception e) {
                    return true;
                }
            }
        });
    }

    public Mono<Boolean> isTokenNearExpiration(String token) {
        return Mono.fromCallable(new java.util.concurrent.Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    Date expiration = extractExpiration(token);
                    long currentTime = System.currentTimeMillis();
                    long expirationTime = expiration.getTime();
                    long fiveMinutes = 5 * 60 * 1000;
                    
                    return (expirationTime - currentTime) <= fiveMinutes;
                } catch (Exception e) {
                    return true;
                }
            }
        });
    }

    public Mono<Long> getTimeToExpiration(String token) {
        return Mono.fromCallable(new java.util.concurrent.Callable<Long>() {
            @Override
            public Long call() {
                try {
                    Date expiration = extractExpiration(token);
                    return expiration.getTime() - System.currentTimeMillis();
                } catch (Exception e) {
                    return 0L;
                }
            }
        });
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpiredSync(String token) {
        return extractExpiration(token).before(new Date());
    }
}