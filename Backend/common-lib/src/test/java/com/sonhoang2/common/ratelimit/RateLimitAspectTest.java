package com.sonhoang2.common.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

    private static final String USER_ID = "3f2b8c1e-7a4d-4e5f-9b6a-1c2d3e4f5a6b";

    private final RateLimitService rateLimitService = mock(RateLimitService.class);

    static class Limited {
        @RateLimit(points = 5, durationSeconds = 60, keyPrefix = "test")
        public void call() {
        }
    }

    private Limited proxied() {
        when(rateLimitService.tryConsume(any(), anyInt(), any())).thenReturn(true);
        AspectJProxyFactory factory = new AspectJProxyFactory(new Limited());
        factory.setProxyTargetClass(true);
        factory.addAspect(new RateLimitAspect(rateLimitService));
        return factory.getProxy();
    }

    private static void currentRequest(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void limitsPerUserFromGatewayHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-User-Id", USER_ID);
        currentRequest(request);

        proxied().call();

        verify(rateLimitService).tryConsume("test:user:" + USER_ID, 5, Duration.ofSeconds(60));
    }

    @Test
    void ignoresClientSuppliedProxyHeadersWithoutUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7");
        request.addHeader("Proxy-Client-IP", "203.0.113.8");
        request.addHeader("WL-Proxy-Client-IP", "203.0.113.9");
        currentRequest(request);

        proxied().call();

        verify(rateLimitService).tryConsume("test:ip:10.0.0.1", 5, Duration.ofSeconds(60));
    }
}
