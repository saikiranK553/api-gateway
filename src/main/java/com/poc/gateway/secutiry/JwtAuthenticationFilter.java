package com.poc.gateway.secutiry;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.gateway.dto.ErrorResponse;
import com.poc.gateway.utils.ReactiveJwtUtil;

import reactor.core.publisher.Mono;

//@Component
//public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
//
//    @Autowired
//    private ReactiveJwtUtil jwtUtil;
//
//    // List of paths that don't require authentication
//    private static final List<String> OPEN_ENDPOINTS = Arrays.asList(
//        "/auth/login",
//        "/auth/register",
//        "/auth/refresh",
//        "/actuator/health"
//    );
//
//    public JwtAuthenticationFilter() {
//        super(Config.class);
//    }
//
//    @Override
//    public GatewayFilter apply(Config config) {
//        return (exchange, chain) -> {
//            ServerHttpRequest request = exchange.getRequest();
//            String path = request.getPath().value();
//            
//            // Skip authentication for open endpoints
//            if (isOpenEndpoint(path)) {
//                return chain.filter(exchange);
//            }
//
//            // Extract JWT token from Authorization header
//            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//            
//            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//                return handleUnauthorized(exchange);
//            }
//
//            String token = authHeader.substring(7); // Remove "Bearer " prefix
//            
//            // Validate token
//            return jwtUtil.validateAccessToken(token)
//                .flatMap(isValid -> {
//                    if (isValid) {
//                        // Add user info to request headers for downstream services
//                        return addUserInfoToRequest(exchange, token)
//                            .then(chain.filter(exchange));
//                    } else {
//                        return handleUnauthorized(exchange);
//                    }
//                })
//                .onErrorResume(error -> handleUnauthorized(exchange));
//        };
//    }
//
//    private boolean isOpenEndpoint(String path) {
//        return OPEN_ENDPOINTS.stream()
//            .anyMatch(endpoint -> path.startsWith(endpoint));
//    }
//
//    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
//        ServerHttpResponse response = exchange.getResponse();
//        response.setStatusCode(HttpStatus.UNAUTHORIZED);
//        return response.setComplete();
//    }
//
//    private Mono<ServerWebExchange> addUserInfoToRequest(ServerWebExchange exchange, String token) {
//        return Mono.zip(
//            jwtUtil.extractEmail(token),
//            jwtUtil.extractUserId(token),
//            jwtUtil.extractRole(token)
//        )
//        .map(tuple -> {
//            String email = tuple.getT1();
//            String userId = tuple.getT2();
//            String role = tuple.getT3();
//            
//            // Create new request with user info headers
//            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
//                .header("X-User-Email", email)
//                .header("X-User-Id", userId)
//                .header("X-User-Role", role)
//                .build();
//            
//            return exchange.mutate().request(mutatedRequest).build();
//        })
//        .onErrorReturn(exchange); // Return original exchange if extraction fails
//    }
//
//    public static class Config {
//        // Configuration properties if needed
//    }
//}

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private ReactiveJwtUtil jwtUtil;

    // List of paths that don't require authentication
    private static final List<String> OPEN_ENDPOINTS = Arrays.asList(
        "/auth/login",
        "/auth/register",
        "/auth/refresh",
        "/actuator/health"
    );

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            
            // Skip authentication for open endpoints
            if (isOpenEndpoint(path)) {
                return chain.filter(exchange);
            }

            // Extract JWT token from Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return handleUnauthorized(exchange);
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            // Validate token
            return jwtUtil.validateAccessToken(token)
                .flatMap(isValid -> {
                    if (isValid) {
                        // Add user info to request headers for downstream services
                        return addUserInfoToRequest(exchange, token)
                            .then(chain.filter(exchange));
                    } else {
                        return handleUnauthorized(exchange);
                    }
                })
                .onErrorResume(error -> handleUnauthorized(exchange));
        };
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream()
            .anyMatch(endpoint -> path.startsWith(endpoint));
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();
        
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            "Invalid or expired token",
            path
        );

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes());
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }

    private Mono<ServerWebExchange> addUserInfoToRequest(ServerWebExchange exchange, String token) {
        return Mono.zip(
            jwtUtil.extractEmail(token),
            jwtUtil.extractUserId(token),
            jwtUtil.extractRole(token)
        )
        .map(tuple -> {
            String email = tuple.getT1();
            String userId = tuple.getT2();
            String role = tuple.getT3();
            
            // Create new request with user info headers
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Email", email)
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build();
            
            return exchange.mutate().request(mutatedRequest).build();
        })
        .onErrorReturn(exchange); // Return original exchange if extraction fails
    }

    public static class Config {
        // Configuration properties if needed
    }
}
