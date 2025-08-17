package com.supermarket.gateway.config;

import com.supermarket.gateway.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JWTUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        System.out.println("** path: " + path);

        if (request.getURI().getPath().startsWith("/user/login") || request.getURI().getPath().startsWith("/user/register")) {
            return chain.filter(exchange); // Don't block login/register
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("invalid auth header");
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            return unauthorized(exchange);
        }

        String username = jwtUtil.extractUsername(token);

        String role = jwtUtil.extractRole(token);
        System.out.println("** extracted role: " + role);
        int userId = jwtUtil.extractUserId(token);

        if (!isAuthorized(path, role)) {
            return forbidden(exchange);
        }
        System.out.println("** path and role validated");

        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate().header("X-Authenticated_User", username).header("X-Role", role).header("X-UserId", String.valueOf(userId)).build();
        System.out.println("** Token validated. User: " + username + ", Role: " + role);
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isAuthorized(String path, String role) {
        System.out.println("** In isAuthorized path: " + path + ", role: " + role);
        System.out.println("** isAuthorized activated");

        // route access for admin-biller-customer
        if (path.startsWith("/bill/admin-biller-customer") && (role.equals("ADMIN") || role.equals("BILLER") || role.equals("CUSTOMER")))
            return true;
        
        if (path.startsWith("/invent/admin-biller-customer") && (role.equals("ADMIN") || role.equals("BILLER") || role.equals("CUSTOMER"))) return true;

        // route access for admin-biller
        if (path.startsWith("/bill/admin-biller") && (role.equals("ADMIN") || role.equals("BILLER"))) return true;

        // route access for admin-customer
        if (path.startsWith("/invent/admin-customer") && (role.equals("ADMIN") || role.equals("CUSTOMER"))) return true;

        if (path.startsWith("/bill/admin-customer") && (role.equals("ADMIN") || role.equals("CUSTOMER"))) return true;

        // route access for biller-customer
        if (path.startsWith("/user/biller-customer") && (role.equals("BILLER") || role.equals("CUSTOMER"))) return true;

        if (path.startsWith("/invent/biller-customer") && (role.equals("BILLER") || role.equals("CUSTOMER")))
            return true;

        if (path.startsWith("/cart/biller-customer") && (role.equals("BILLER") || role.equals("CUSTOMER"))) return true;

        if (path.startsWith("/bill/biller-customer") && (role.equals("BILLER") || role.equals("CUSTOMER"))) return true;

        if (path.startsWith("/payment/biller-customer") && (role.equals("BILLER") || role.equals("CUSTOMER")))
            return true;

        // route access for admin only
        if (path.startsWith("/user/admin") && role.equals("ADMIN")) return true;

        if (path.startsWith("/invent/admin") && role.equals("ADMIN")) return true;

        if (path.startsWith("/bill/admin") && role.equals("ADMIN")) return true;

        if (path.startsWith("/payment/admin") && role.equals("ADMIN")) return true;

        if (path.startsWith("/cart/admin") && role.equals("ADMIN")) return true;

        // route access for biller
        if (path.startsWith("/user/biller") && role.equals("BILLER")) return true;

        if (path.startsWith("/invent/biller") && role.equals("BILLER")) return true;

        if (path.startsWith("/cart/biller") && role.equals("BILLER")) return true;

        if (path.startsWith("/bill/biller") && role.equals("BILLER")) return true;

        // route access for customer
        if (path.startsWith("/cart/customer") && role.equals("CUSTOMER")) return true;

        if (path.startsWith("/invent/customer") && role.equals("CUSTOMER")) return true;

        if (path.startsWith("/bill/customer") && role.equals("CUSTOMER")) return true;

        if (path.startsWith("/payment/customer") && role.equals("CUSTOMER")) return true;

        return false;
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        System.out.println("** forbidden activated");
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        System.out.println("** unauthorized activated");
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
