package com.poc.gateway.config;

import java.util.Arrays;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.poc.gateway.secutiry.DownstreamResponseFilter;
import com.poc.gateway.secutiry.JwtAuthenticationFilter;
import com.poc.gateway.secutiry.ResponseErrorFilter;

//@Configuration
//public class GatewayConfig {
//
//    @Bean
//    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, 
//                                          JwtAuthenticationFilter jwtAuthFilter) {
//        return builder.routes()
//            // Auth service routes (no JWT validation needed)
//            .route("auth-service", r -> r.path("/auth/**")
//                .uri("lb://auth-service"))
//            
//            // Profile service routes (JWT validation required)
//            .route("profile-service", r -> r.path("/profile/**")
//                .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config())))
//                .uri("lb://profile-service"))
//            
//            // Other protected service routes
//            .route("protected-service", r -> r.path("/api/**")
//                .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config())))
//                .uri("lb://protected-service"))
//            
//            // Health check route (no JWT validation)
//            .route("health-check", r -> r.path("/actuator/health")
//                .uri("lb://health-service"))
//            
//            .build();
//    }
//
//    @Bean
//    public CorsWebFilter corsWebFilter() {
//        CorsConfiguration corsConfiguration = new CorsConfiguration();
//        corsConfiguration.setAllowedOriginPatterns(Arrays.asList("*"));
//        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
//        corsConfiguration.setAllowCredentials(true);
//        
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", corsConfiguration);
//        
//        return new CorsWebFilter(source);
//    }
//}

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, 
                                          JwtAuthenticationFilter jwtAuthFilter,
                                          ResponseErrorFilter responseErrorFilter,
                                          DownstreamResponseFilter downstreamResponseFilter) {
        return builder.routes()
            // Auth service routes (no JWT validation needed)
            .route("auth-service", r -> r.path("/api/auth/**")
                .filters(f -> f.filter(responseErrorFilter.apply(new ResponseErrorFilter.Config()))
                              .filter(downstreamResponseFilter.apply(new DownstreamResponseFilter.Config())))
                //.uri("lb://auth-service"))
                .uri("http://localhost:8081"))

            
            // Profile service routes (JWT validation required)
            .route("profile-service", r -> r.path("/api/users/**")
                .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()))
                              .filter(responseErrorFilter.apply(new ResponseErrorFilter.Config()))
                              .filter(downstreamResponseFilter.apply(new DownstreamResponseFilter.Config())))
                //.uri("lb://profile-service"))
                .uri("http://localhost:8080"))
            
            // Health check route (no JWT validation)
            .route("health-check", r -> r.path("/actuator/health")
                .filters(f -> f.filter(responseErrorFilter.apply(new ResponseErrorFilter.Config())))
                .uri("lb://health-service"))
            
            .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(Arrays.asList("*"));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
        corsConfiguration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        
        return new CorsWebFilter(source);
    }
}