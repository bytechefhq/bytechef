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

package com.bytechef.component.ai.agent.chat.memory.builtin.session.util;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.BuiltInSessionRepositoryFactory.BuiltInSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.redis.RedisSessionRepository;
import org.springframework.mock.env.MockEnvironment;
import redis.clients.jedis.JedisPooled;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
class BuiltInSessionRepositoryFactoryTest {

    @Test
    void testDefaultProviderWithoutDataSourceFallsBackToInMemory() {
        BuiltInSessionRepository builtInSessionRepository = BuiltInSessionRepositoryFactory.create(
            new MockEnvironment(), null);

        assertInstanceOf(InMemorySessionRepository.class, builtInSessionRepository.sessionRepository());
        assertNull(builtInSessionRepository.closeable());
    }

    @Test
    void testInMemoryProvider() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "in_memory");

        BuiltInSessionRepository builtInSessionRepository = BuiltInSessionRepositoryFactory.create(environment, null);

        assertInstanceOf(InMemorySessionRepository.class, builtInSessionRepository.sessionRepository());
        assertNull(builtInSessionRepository.closeable());
    }

    @Test
    void testRedisProvider() throws Exception {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "redis")
            .withProperty("bytechef.ai.memory.redis.host", "localhost")
            .withProperty("bytechef.ai.memory.redis.port", "6379");

        BuiltInSessionRepository builtInSessionRepository = BuiltInSessionRepositoryFactory.create(environment, null);

        try {
            assertInstanceOf(RedisSessionRepository.class, builtInSessionRepository.sessionRepository());
            assertInstanceOf(JedisPooled.class, builtInSessionRepository.closeable());
        } finally {
            if (builtInSessionRepository.closeable() != null) {
                builtInSessionRepository.closeable()
                    .close();
            }
        }
    }

    @Test
    void testRedisProviderRequiresPasswordWhenUsernameIsConfigured() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "redis")
            .withProperty("bytechef.ai.memory.redis.username", "user");

        assertThrows(
            IllegalArgumentException.class, () -> BuiltInSessionRepositoryFactory.create(environment, null));
    }

    @Test
    void testAwsProvider() throws Exception {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "aws")
            .withProperty("bytechef.ai.memory.aws.region", "us-east-1")
            .withProperty("bytechef.ai.memory.aws.access-key-id", "test-access-key")
            .withProperty("bytechef.ai.memory.aws.secret-access-key", "test-secret-key");

        BuiltInSessionRepository builtInSessionRepository = BuiltInSessionRepositoryFactory.create(environment, null);

        try {
            assertInstanceOf(TenantRoutingS3SessionRepository.class, builtInSessionRepository.sessionRepository());
            assertInstanceOf(S3Client.class, builtInSessionRepository.closeable());
        } finally {
            if (builtInSessionRepository.closeable() != null) {
                builtInSessionRepository.closeable()
                    .close();
            }
        }
    }
}
