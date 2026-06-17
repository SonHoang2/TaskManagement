package com.sonhoang2.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtToHeaderGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal().cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> {
                    Jwt jwt = authentication.getToken();
                    String userId = jwt.getClaimAsString("userId");
                    if (userId == null) {
                        userId = jwt.getClaimAsString("sub");
                    }
                    if (userId == null) {
                        userId = jwt.getClaimAsString("user_id");
                    }
                    if (userId != null) {
                        ServerHttpRequest newRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", userId)
                                .headers(httpHeaders -> httpHeaders.remove("Authorization"))
                                .build();
                        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
                        return chain.filter(newExchange);
                    } else {
                        return chain.filter(exchange);
                    }
                }).switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}