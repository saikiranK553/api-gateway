package com.poc.gateway.secutiry;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.gateway.dto.ErrorResponse;

import reactor.core.publisher.Mono;

@Component
public class ResponseErrorFilter extends AbstractGatewayFilterFactory<ResponseErrorFilter.Config> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseErrorFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange).doOnError(throwable -> {
                handleDownstreamError(exchange, throwable);
            }).onErrorResume(throwable -> {
                return handleDownstreamError(exchange, throwable);
            });
        };
    }

    
    private Mono<Void> handleDownstreamError(ServerWebExchange exchange, Throwable throwable) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();
        
        if (throwable instanceof org.springframework.web.server.ResponseStatusException) {
            org.springframework.web.server.ResponseStatusException ex = 
                (org.springframework.web.server.ResponseStatusException) throwable;
            
            HttpStatus httpStatus = HttpStatus.resolve(ex.getStatusCode().value());
            if (httpStatus == null) {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            
            return writeErrorResponse(response, httpStatus, ex.getReason(), path);
        }
        
        if (throwable.getMessage() != null && 
            (throwable.getMessage().contains("Connection refused") || 
             throwable.getMessage().contains("Service unavailable"))) {
            return writeErrorResponse(response, HttpStatus.SERVICE_UNAVAILABLE, 
                "Service temporarily unavailable", path);
        }
        
        if (throwable instanceof java.util.concurrent.TimeoutException) {
            return writeErrorResponse(response, HttpStatus.GATEWAY_TIMEOUT, 
                "Request timeout", path);
        }
        
        // Generic error
        return writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, 
            "Internal server error", path);
    }

    private Mono<Void> writeErrorResponse(ServerHttpResponse response, HttpStatus status, 
                                         String message, String path) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse(
            status.value(),
            status.getReasonPhrase(),
            message,
            path
        );

        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            String fallbackResponse = "{\"status\":" + status.value() + 
                ",\"error\":\"" + status.getReasonPhrase() + 
                "\",\"message\":\"" + message + 
                "\",\"path\":\"" + path + "\"}";
            DataBuffer buffer = response.bufferFactory().wrap(fallbackResponse.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    public static class Config {
    }
}
