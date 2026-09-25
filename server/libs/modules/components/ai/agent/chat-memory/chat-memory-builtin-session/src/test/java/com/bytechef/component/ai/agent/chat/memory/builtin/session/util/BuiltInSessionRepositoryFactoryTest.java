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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.BuiltInSessionRepositoryFactory.BuiltInSessionRepository;
import com.bytechef.tenant.TenantContext;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionRepository;
import org.springframework.mock.env.MockEnvironment;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author Ivica Cardic
 */
class BuiltInSessionRepositoryFactoryTest {

    @Test
    void testDefaultProviderWithoutDataSourceFallsBackToTenantScopedInMemory() {
        BuiltInSessionRepository builtInSessionRepository = BuiltInSessionRepositoryFactory.create(
            new MockEnvironment(), null);

        assertNull(builtInSessionRepository.closeable());
        assertSessionsAreIsolatedPerTenant(builtInSessionRepository.sessionRepository());
    }

    @Test
    void testInMemoryProviderIsTenantScoped() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "in_memory");

        BuiltInSessionRepository builtInSessionRepository = BuiltInSessionRepositoryFactory.create(environment, null);

        assertNull(builtInSessionRepository.closeable());
        assertSessionsAreIsolatedPerTenant(builtInSessionRepository.sessionRepository());
    }

    @Test
    void testRedisProvider() throws Exception {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "redis")
            .withProperty("bytechef.ai.memory.redis.host", "localhost")
            .withProperty("bytechef.ai.memory.redis.port", "6379");

        BuiltInSessionRepository builtInSessionRepository = BuiltInSessionRepositoryFactory.create(environment, null);

        try {
            assertInstanceOf(TenantRoutingSessionRepository.class, builtInSessionRepository.sessionRepository());
            assertInstanceOf(JedisPooled.class, builtInSessionRepository.closeable());
        } finally {
            AutoCloseable closeable = builtInSessionRepository.closeable();

            if (closeable != null) {
                closeable.close();
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
            AutoCloseable closeable = builtInSessionRepository.closeable();

            if (closeable != null) {
                closeable.close();
            }
        }
    }

    @Test
    void testRedisSessionKeysAreScopedPerTenant() {
        UnifiedJedis jedis = mock(UnifiedJedis.class);

        SessionRepository sessionRepository = BuiltInSessionRepositoryFactory.createRedisSessionRepository(
            jedis, "bytechef-session:");

        TenantContext.runWithTenantId("tenantA", () -> sessionRepository.findById("session-1"));
        TenantContext.runWithTenantId("tenantB", () -> sessionRepository.findById("session-1"));

        verify(jedis).get("bytechef-session:tenantA:session-1");
        verify(jedis).get("bytechef-session:tenantB:session-1");
    }

    private static void assertSessionsAreIsolatedPerTenant(SessionRepository sessionRepository) {
        TenantContext.runWithTenantId("tenantA", () -> sessionRepository.save(Session.builder()
            .id("shared-session-id")
            .userId("user-1")
            .createdAt(Instant.now())
            .build()));

        Session tenantASession = TenantContext.callWithTenantId(
            "tenantA", () -> sessionRepository.findById("shared-session-id"));
        Session tenantBSession = TenantContext.callWithTenantId(
            "tenantB", () -> sessionRepository.findById("shared-session-id"));

        assertNotNull(tenantASession);
        assertNull(tenantBSession);
    }
}
