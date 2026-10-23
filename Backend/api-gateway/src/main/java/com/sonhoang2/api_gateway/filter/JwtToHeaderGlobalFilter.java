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
    public Mono<Void> filter(ServerWebExchange incoming, GatewayFilterChain chain) {
        // Services trust X-User-Id, so a client-supplied value must never reach them (e.g. on permitAll routes)
        ServerWebExchange exchange = incoming.mutate()
                .request(incoming.getRequest().mutate().headers(headers -> headers.remove("X-User-Id")).build())
                .build();
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