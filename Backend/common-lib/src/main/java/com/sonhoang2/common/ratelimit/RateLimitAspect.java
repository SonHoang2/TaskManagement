package com.sonhoang2.common.ratelimit;

import com.sonhoang2.common.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = rateLimit.keyPrefix() + ":" + resolveKey();
        boolean allowed = rateLimitService.tryConsume(
                key, rateLimit.points(), Duration.ofSeconds(rateLimit.durationSeconds())
        );
        if (!allowed) {
            throw new RateLimitExceededException("Too many requests");
        }
        return pjp.proceed();
    }

    private String resolveKey() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();

        // The gateway sets X-User-Id from the verified JWT (and drops any client-supplied value),
        // the same header services already trust for authorization.
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }

        // X-Forwarded-For and similar headers are client-controlled; only the socket address cannot be spoofed.
        return "ip:" + request.getRemoteAddr();
    }
}
