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
public class DownstreamResponseFilter extends AbstractGatewayFilterFactory<DownstreamResponseFilter.Config> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DownstreamResponseFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    HttpStatus statusCode = (HttpStatus) response.getStatusCode();
                    
                    if (statusCode != null && statusCode.isError()) {
                        processErrorResponse(exchange, statusCode);
                    }
                })
            );
        };
    }

    private void processErrorResponse(ServerWebExchange exchange, HttpStatus statusCode) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();
        
        if (response.isCommitted()) {
            return;
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            statusCode.value(),
            statusCode.getReasonPhrase(),
            getErrorMessage(statusCode),
            path
        );
        
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            
            response.setStatusCode(statusCode);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            System.err.println("Error processing downstream response: " + e.getMessage());
        }
    }

    private String getErrorMessage(HttpStatus statusCode) {
        switch (statusCode) {
            case UNAUTHORIZED:
                return "Authentication required";
            case FORBIDDEN:
                return "Access denied";
            case NOT_FOUND:
                return "Resource not found";
            case BAD_REQUEST:
                return "Invalid request";
            case INTERNAL_SERVER_ERROR:
                return "Internal server error";
            case SERVICE_UNAVAILABLE:
                return "Service temporarily unavailable";
            case GATEWAY_TIMEOUT:
                return "Request timeout";
            default:
                return statusCode.getReasonPhrase();
        }
    }

    public static class Config {
    }
}
