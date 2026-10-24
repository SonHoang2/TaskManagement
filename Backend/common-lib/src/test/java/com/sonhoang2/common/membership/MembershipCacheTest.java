package com.sonhoang2.common.membership;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipCacheTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> values;
    private MembershipCache cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        cache = new MembershipCache(redisTemplate, new ObjectMapper());
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void getReturnsCachedMemberships() {
        when(values.get(MembershipCache.key(userId))).thenReturn("{\"" + projectId + "\":\"OWNER\"}");

        assertThat(cache.get(userId)).contains(Map.of(projectId, "OWNER"));
    }

    @Test
    void getMissesWhenKeyIsAbsent() {
        assertThat(cache.get(userId)).isEmpty();
    }

    @Test
    void getMissesWhenRedisIsDown() {
        when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(cache.get(userId)).isEmpty();
    }

    @Test
    void putWritesJsonWithTtl() {
        cache.put(userId, Map.of(projectId, "MEMBER"));

        verify(values).set(MembershipCache.key(userId), "{\"" + projectId + "\":\"MEMBER\"}", MembershipCache.TTL);
    }

    @Test
    void putSwallowsRedisFailures() {
        doThrow(new RedisConnectionFailureException("down"))
                .when(values).set(anyString(), anyString(), any(Duration.class));

        cache.put(userId, Map.of(projectId, "MEMBER"));
    }

    @Test
    void evictOutsideTransactionDeletesAtOnce() {
        cache.evictAfterCommit(List.of(userId));

        verify(redisTemplate).delete(List.of(MembershipCache.key(userId)));
    }

    @Test
    void evictInsideTransactionWaitsForCommit() {
        TransactionSynchronizationManager.initSynchronization();

        cache.evictAfterCommit(List.of(userId));
        verify(redisTemplate, never()).delete(anyCollection());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(redisTemplate).delete(List.of(MembershipCache.key(userId)));
    }
}
