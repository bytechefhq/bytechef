/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.chat.memory.redis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * @author Ivica Cardic
 */
class RedisChatMemoryTenantTeardownContributorTest {

    private final RedisClient redisClient = mock(RedisClient.class);
    private final RedisChatMemoryTenantTeardownContributor contributor =
        new RedisChatMemoryTenantTeardownContributor(redisClient);

    @Test
    void testDropsIndexAndDeletesKeysOfTheTenant() {
        when(redisClient.scan(eq("0"), any(ScanParams.class)))
            .thenReturn(new ScanResult<>("17", List.of("chat-memory-000001:a", "chat-memory-000001:b")));
        when(redisClient.scan(eq("17"), any(ScanParams.class)))
            .thenReturn(new ScanResult<>("0", List.of("chat-memory-000001:c")));

        contributor.afterSchemaDrop("000001");

        verify(redisClient).ftDropIndex(TenantRoutingRedisChatMemoryRepository.getIndexName("000001"));

        ArgumentCaptor<ScanParams> scanParamsCaptor = ArgumentCaptor.forClass(ScanParams.class);

        verify(redisClient).scan(eq("0"), scanParamsCaptor.capture());

        assertThat(scanParamsCaptor.getValue()
            .match()).isEqualTo("chat-memory-000001:*");

        verify(redisClient).unlink("chat-memory-000001:a", "chat-memory-000001:b");
        verify(redisClient).unlink(new String[] {
            "chat-memory-000001:c"
        });
    }

    @Test
    void testMissingIndexIsNoOp() {
        doThrow(new JedisDataException("Unknown index name")).when(redisClient)
            .ftDropIndex(any(String.class));
        when(redisClient.scan(eq("0"), any(ScanParams.class))).thenReturn(new ScanResult<>("0", List.of()));

        contributor.afterSchemaDrop("000001");

        verify(redisClient).scan(eq("0"), any(ScanParams.class));
    }

    @Test
    void testUnexpectedIndexErrorPropagates() {
        doThrow(new JedisDataException("OOM")).when(redisClient)
            .ftDropIndex(any(String.class));

        assertThatThrownBy(() -> contributor.afterSchemaDrop("000001"))
            .isInstanceOf(JedisDataException.class);
    }

    @Test
    void testRejectsInvalidTenantId() {
        assertThatThrownBy(() -> contributor.afterSchemaDrop("a*"))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(redisClient);
    }

    @Test
    void testDefaultTenantIsNeverTouched() {
        assertThatThrownBy(() -> contributor.afterSchemaDrop("public"))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(redisClient);
    }
}
