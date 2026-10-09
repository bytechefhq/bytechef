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

package com.bytechef.component.ai.agent.chat.memory.builtin.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.chat.memory.builtin.util.BuiltInSessionRepositoryFactory.BuiltInSessionStore;
import com.bytechef.platform.component.definition.ai.agent.TenantRoutingSessionRepository;
import com.bytechef.tenant.TenantContext;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.mock.env.MockEnvironment;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.UnifiedJedis;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;

/**
 * @author Ivica Cardic
 */
class BuiltInSessionRepositoryFactoryTest {

    @Test
    void testMissingProviderFailsFast() {
        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class, () -> BuiltInSessionRepositoryFactory.create(new MockEnvironment(), null));

        assertTrue(illegalStateException.getMessage()
            .contains("bytechef.ai.memory.provider"));
    }

    @Test
    void testBuiltInSessionStoreRejectsANullSessionRepository() {
        assertThrows(NullPointerException.class, () -> new BuiltInSessionStore(null, () -> {}));
    }

    @Test
    void testUnknownProviderFailsFast() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "mongodb");

        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class, () -> BuiltInSessionRepositoryFactory.create(environment, null));

        assertTrue(illegalStateException.getMessage()
            .contains("Unknown bytechef.ai.memory.provider value 'mongodb'"));
    }

    @Test
    void testJdbcProviderWithoutDataSourceFailsOnUse() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "jdbc");

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(environment, null);

        SessionRepository sessionRepository = builtInSessionStore.sessionRepository();

        assertInstanceOf(UnavailableSessionRepository.class, sessionRepository);
        assertDoesNotThrow(builtInSessionStore.closeable()::close);

        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class, () -> sessionRepository.findById("conversation-1"));

        assertTrue(illegalStateException.getMessage()
            .contains("bytechef.ai.memory.provider=jdbc requires a DataSource"));
    }

    @Test
    void testInMemoryProviderIsTenantScoped() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "in_memory");

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(environment, null);

        assertDoesNotThrow(builtInSessionStore.closeable()::close);
        assertSessionsAreIsolatedPerTenant(builtInSessionStore.sessionRepository());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "in_memory", "IN_MEMORY", "In_Memory"
    })
    void testProviderNameIsMatchedIgnoringCase(String providerName) {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", providerName);

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(environment, null);

        assertSessionsAreIsolatedPerTenant(builtInSessionStore.sessionRepository());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "inmemory", "in-memory"
    })
    void testProviderNameOtherThanTheExactValueIsRejected(String providerName) {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", providerName);

        IllegalStateException illegalStateException = assertThrows(
            IllegalStateException.class, () -> BuiltInSessionRepositoryFactory.create(environment, null));

        assertTrue(illegalStateException.getMessage()
            .contains("expected one of aws, in_memory, jdbc or redis"));
    }

    @Test
    void testUppercaseJdbcProviderIsRecognized() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "JDBC");

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(environment, null);

        assertInstanceOf(UnavailableSessionRepository.class, builtInSessionStore.sessionRepository());
    }

    @Test
    void testMemoryAwsCredentialsTakePrecedenceOverCloudCredentials() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.cloud.aws.access-key-id", "cloud-access-key")
            .withProperty("bytechef.cloud.aws.secret-access-key", "cloud-secret-key")
            .withProperty("bytechef.ai.memory.aws.access-key-id", "memory-access-key")
            .withProperty("bytechef.ai.memory.aws.secret-access-key", "memory-secret-key");

        AwsCredentials awsCredentials = resolveCredentials(environment);

        assertEquals("memory-access-key", awsCredentials.accessKeyId());
        assertEquals("memory-secret-key", awsCredentials.secretAccessKey());
    }

    @Test
    void testCloudAwsCredentialsAreUsedWhenMemoryCredentialsAreNotSet() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.cloud.aws.access-key-id", "cloud-access-key")
            .withProperty("bytechef.cloud.aws.secret-access-key", "cloud-secret-key");

        AwsCredentials awsCredentials = resolveCredentials(environment);

        assertEquals("cloud-access-key", awsCredentials.accessKeyId());
        assertEquals("cloud-secret-key", awsCredentials.secretAccessKey());
    }

    @Test
    void testHalfConfiguredMemoryAwsCredentialsAreRejected() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.cloud.aws.access-key-id", "cloud-access-key")
            .withProperty("bytechef.cloud.aws.secret-access-key", "cloud-secret-key")
            .withProperty("bytechef.ai.memory.aws.access-key-id", "memory-access-key")
            .withProperty("bytechef.ai.memory.aws.secret-access-key", " ");

        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> BuiltInSessionRepositoryFactory.getAwsCredentialsProvider(environment));

        assertTrue(illegalArgumentException.getMessage()
            .contains("bytechef.ai.memory.aws.secret-access-key"));
    }

    @Test
    void testDefaultCredentialsProviderIsUsedWhenNoCredentialsAreSet() {
        assertInstanceOf(
            DefaultCredentialsProvider.class,
            BuiltInSessionRepositoryFactory.getAwsCredentialsProvider(new MockEnvironment()));
    }

    @Test
    void testAwsRegionFallsBackToCloudAws() throws Exception {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "aws")
            .withProperty("bytechef.cloud.aws.region", "eu-north-1")
            .withProperty("bytechef.ai.memory.aws.key-prefix", "")
            .withProperty("bytechef.ai.memory.aws.session-bucket-prefix", "bytechef-session")
            .withProperty("bytechef.ai.memory.session-max-archived-events", "1000");

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(environment, null);

        try (S3Client s3Client = assertInstanceOf(S3Client.class, builtInSessionStore.closeable())) {
            S3ServiceClientConfiguration s3ServiceClientConfiguration = s3Client.serviceClientConfiguration();

            assertEquals(Region.EU_NORTH_1, s3ServiceClientConfiguration.region());
        }
    }

    private static AwsCredentials resolveCredentials(MockEnvironment environment) {
        AwsCredentialsProvider awsCredentialsProvider = BuiltInSessionRepositoryFactory.getAwsCredentialsProvider(
            environment);

        return awsCredentialsProvider.resolveCredentials();
    }

    @Test
    void testRedisProvider() throws Exception {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "redis")
            .withProperty("bytechef.ai.memory.redis.host", "localhost")
            .withProperty("bytechef.ai.memory.redis.session-key-prefix", "bytechef-session:")
            .withProperty("bytechef.ai.memory.redis.port", "6379")
            .withProperty("bytechef.ai.memory.session-max-archived-events", "1000");

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(environment, null);

        try {
            assertInstanceOf(TenantRoutingSessionRepository.class, builtInSessionStore.sessionRepository());
            assertInstanceOf(RedisClient.class, builtInSessionStore.closeable());
        } finally {
            AutoCloseable closeable = builtInSessionStore.closeable();

            closeable.close();
        }
    }

    @Test
    void testRedisProviderRequiresPasswordWhenUsernameIsConfigured() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "redis")
            .withProperty("bytechef.ai.memory.redis.host", "localhost")
            .withProperty("bytechef.ai.memory.redis.port", "6379")
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
            .withProperty("bytechef.ai.memory.aws.secret-access-key", "test-secret-key")
            .withProperty("bytechef.ai.memory.aws.key-prefix", "")
            .withProperty("bytechef.ai.memory.aws.session-bucket-prefix", "bytechef-session")
            .withProperty("bytechef.ai.memory.session-max-archived-events", "1000");

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(environment, null);

        try {
            assertInstanceOf(TenantRoutingSessionRepository.class, builtInSessionStore.sessionRepository());
            assertInstanceOf(S3Client.class, builtInSessionStore.closeable());
        } finally {
            AutoCloseable closeable = builtInSessionStore.closeable();

            closeable.close();
        }
    }

    @Test
    void testRedisNegativeMaxArchivedEventsIsRejectedAtConstruction() {
        UnifiedJedis jedis = mock(UnifiedJedis.class);

        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> BuiltInSessionRepositoryFactory.createRedisSessionRepository(jedis, "bytechef-session:", -1));

        assertTrue(illegalArgumentException.getMessage()
            .contains("bytechef.ai.memory.session-max-archived-events"));
    }

    @Test
    void testRedisSessionKeysAreScopedPerTenant() {
        UnifiedJedis jedis = mock(UnifiedJedis.class);

        when(jedis.hmget(anyString(), any(String[].class))).thenReturn(Arrays.asList(null, null));

        SessionRepository sessionRepository = BuiltInSessionRepositoryFactory.createRedisSessionRepository(
            jedis, "bytechef-session:", 1000);

        TenantContext.runWithTenantId("tenantA", () -> sessionRepository.findById("session-1"));
        TenantContext.runWithTenantId("tenantB", () -> sessionRepository.findById("session-1"));

        verify(jedis).hmget("bytechef-session:tenantA:session-1", "document", "revision");
        verify(jedis).hmget("bytechef-session:tenantB:session-1", "document", "revision");
    }

    @Test
    void testRedisProviderPrunesArchivedEventsBeyondTheConfiguredLimit() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "redis")
            .withProperty("bytechef.ai.memory.redis.session-key-prefix", "bytechef-session:")
            .withProperty("bytechef.ai.memory.session-max-archived-events", "1");

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(
            environment, null, ignoredEnvironment -> createRedisClient(), ignoredEnvironment -> mock(S3Client.class));

        assertEquals(1, countArchivedEventsAfterAppendingThree(builtInSessionStore.sessionRepository()));
    }

    @Test
    void testAwsProviderPrunesArchivedEventsBeyondTheConfiguredLimit() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("bytechef.ai.memory.provider", "aws")
            .withProperty("bytechef.ai.memory.aws.key-prefix", "")
            .withProperty("bytechef.ai.memory.aws.session-bucket-prefix", "bytechef-session")
            .withProperty("bytechef.ai.memory.session-max-archived-events", "1");

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(
            environment, null, ignoredEnvironment -> mock(RedisClient.class),
            ignoredEnvironment -> new InMemoryS3Client().getS3Client());

        assertEquals(1, countArchivedEventsAfterAppendingThree(builtInSessionStore.sessionRepository()));
    }

    private static long countArchivedEventsAfterAppendingThree(SessionRepository sessionRepository) {
        return TenantContext.callWithTenantId("tenanta", () -> {
            sessionRepository.save(Session.builder()
                .id("session-1")
                .userId("user-1")
                .createdAt(Instant.now())
                .build());

            for (int index = 0; index < 3; index++) {
                sessionRepository.appendEvent(SessionEvent.builder()
                    .sessionId("session-1")
                    .message(new UserMessage("archived message " + index))
                    .archived(true)
                    .build());
            }

            List<SessionEvent> sessionEvents = sessionRepository.findEvents("session-1", EventFilter.all());

            return sessionEvents.stream()
                .filter(SessionEvent::isArchived)
                .count();
        });
    }

    private static RedisClient createRedisClient() {
        Map<String, String> documents = new ConcurrentHashMap<>();

        RedisClient redisClient = mock(RedisClient.class);

        when(redisClient.hmget(anyString(), any(String[].class)))
            .thenAnswer(invocation -> {
                String document = documents.get(invocation.getArgument(0, String.class));

                return Arrays.asList(document, document == null ? null : "1");
            });
        when(redisClient.eval(anyString(), anyList(), anyList()))
            .thenAnswer(invocation -> {
                List<String> keys = invocation.getArgument(1);
                List<String> arguments = invocation.getArgument(2);

                documents.put(keys.getFirst(), arguments.getFirst());

                return 1L;
            });

        return redisClient;
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
