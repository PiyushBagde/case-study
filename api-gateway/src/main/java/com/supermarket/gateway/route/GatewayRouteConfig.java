package com.supermarket.gateway.route;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

                // User Service
                .route("user-service", r -> r.path("/user/**")
                        .uri("http://localhost:8081"))

                // Inventory Service
                .route("inventory-service", r -> r.path("/invent/**")
                        .uri("http://localhost:8082"))

                // Cart Service
                .route("cart-service", r -> r.path("/cart/**")
                        .uri("http://localhost:8083"))

                // Billing Service
                .route("bill-service", r -> r.path("/bill/**")
                        .uri("http://localhost:8084"))

                // Payment Service
                .route("payment-service", r -> r.path("/payment/**")
                        .uri("http://localhost:8085"))

                .build();
    }
}