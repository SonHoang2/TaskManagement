package com.sonhoang2.common.membership;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A user's project memberships (projectId -> role), cached in Redis and shared by every replica.
 * Readers (task-service) fill it from project-service; project-service evicts a user's entry whenever
 * their memberships change, so the change is visible everywhere at once. The TTL is only a safety net.
 * Redis failures never fail the caller: reads miss, writes and evictions are logged and skipped.
 * Registered by {@link com.sonhoang2.common.autoconfigure.MembershipCacheAutoConfiguration}.
 */
@Slf4j
public class MembershipCache {

    static final Duration TTL = Duration.ofSeconds(60);
    private static final String KEY_PREFIX = "project-membership:";
    private static final TypeReference<Map<UUID, String>> TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MembershipCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public static String key(UUID userId) {
        return KEY_PREFIX + userId;
    }

    /**
     * Empty on a miss or when Redis is unavailable; the caller then loads from project-service.
     */
    public Optional<Map<UUID, String>> get(UUID userId) {
        try {
            String json = redisTemplate.opsForValue().get(key(userId));
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, TYPE));
        } catch (Exception e) {
            log.warn("Membership cache read failed for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(UUID userId, Map<UUID, String> memberships) {
        try {
            redisTemplate.opsForValue().set(key(userId), objectMapper.writeValueAsString(memberships), TTL);
        } catch (Exception e) {
            log.warn("Membership cache write failed for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Evicts once the surrounding transaction commits. Evicting earlier would let a reader fetch the
     * not-yet-committed (old) memberships and cache them again.
     */
    public void evictAfterCommit(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        List<UUID> ids = List.copyOf(userIds);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(ids);
                }
            });
        } else {
            evict(ids);
        }
    }

    private void evict(List<UUID> userIds) {
        try {
            redisTemplate.delete(userIds.stream().map(MembershipCache::key).toList());
        } catch (Exception e) {
            log.warn("Membership cache eviction failed for users {}: {}", userIds, e.getMessage());
        }
    }
}
