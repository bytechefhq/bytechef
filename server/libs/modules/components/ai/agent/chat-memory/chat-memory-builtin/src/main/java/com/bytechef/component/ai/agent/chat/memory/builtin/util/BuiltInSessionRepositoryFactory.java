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

import com.bytechef.component.ai.agent.chat.memory.jdbc.util.SessionChatMemoryUtils;
import com.bytechef.platform.component.definition.ai.agent.TenantRoutingSessionRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.redis.RedisSessionRepository;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.UnifiedJedis;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * @author Ivica Cardic
 */
public final class BuiltInSessionRepositoryFactory {

    private static final String CLOUD_AWS_PREFIX = "bytechef.cloud.aws.";
    private static final String MEMORY_AWS_PREFIX = "bytechef.ai.memory.aws.";
    private static final String PROVIDER = "bytechef.ai.memory.provider";
    private static final String SESSION_MAX_ARCHIVED_EVENTS = "bytechef.ai.memory.session-max-archived-events";

    private static final Logger log = LoggerFactory.getLogger(BuiltInSessionRepositoryFactory.class);

    private BuiltInSessionRepositoryFactory() {
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public record BuiltInSessionStore(SessionRepository sessionRepository, AutoCloseable closeable) {

        private static final AutoCloseable NO_OP_CLOSEABLE = () -> {};

        public BuiltInSessionStore {
            Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");

            closeable = Objects.requireNonNullElse(closeable, NO_OP_CLOSEABLE);
        }

        BuiltInSessionStore(SessionRepository sessionRepository) {
            this(sessionRepository, NO_OP_CLOSEABLE);
        }
    }

    private enum Provider {

        AWS, IN_MEMORY, JDBC, REDIS;

        static Provider parse(String value) {
            for (Provider provider : values()) {
                String providerName = provider.name();

                if (providerName.equalsIgnoreCase(value)) {
                    return provider;
                }
            }

            throw new IllegalStateException(
                "Unknown " + PROVIDER + " value '" + value + "'; expected one of aws, in_memory, jdbc or redis");
        }
    }

    private record ResolvedAwsCredentials(AwsCredentialsProvider awsCredentialsProvider, String source) {
    }

    public static BuiltInSessionStore create(Environment environment, @Nullable JdbcTemplate jdbcTemplate) {
        return create(
            environment, jdbcTemplate, BuiltInSessionRepositoryFactory::buildRedisClient,
            BuiltInSessionRepositoryFactory::buildS3Client);
    }

    static BuiltInSessionStore create(
        Environment environment, @Nullable JdbcTemplate jdbcTemplate,
        Function<Environment, RedisClient> redisClientFactory, Function<Environment, S3Client> s3ClientFactory) {

        Provider provider = Provider.parse(environment.getRequiredProperty(PROVIDER));

        return switch (provider) {
            case AWS -> createS3SessionRepository(environment, s3ClientFactory);
            case REDIS -> createRedisSessionRepository(environment, redisClientFactory);
            case IN_MEMORY -> new BuiltInSessionStore(createInMemorySessionRepository());
            case JDBC -> createJdbcSessionRepository(jdbcTemplate);
        };
    }

    private static SessionRepository createInMemorySessionRepository() {
        return new TenantRoutingSessionRepository(tenantId -> InMemorySessionRepository.builder()
            .build());
    }

    private static BuiltInSessionStore createJdbcSessionRepository(@Nullable JdbcTemplate jdbcTemplate) {
        DataSource dataSource = jdbcTemplate == null ? null : jdbcTemplate.getDataSource();

        if (dataSource == null) {
            log.warn(
                "Built-in session chat memory is unavailable: bytechef.ai.memory.provider=jdbc but no DataSource is " +
                    "configured; configure one or choose the aws, redis or in_memory provider");

            return new BuiltInSessionStore(
                new UnavailableSessionRepository(
                    "bytechef.ai.memory.provider=jdbc requires a DataSource; configure one or choose the aws, redis " +
                        "or in_memory provider"));
        }

        return new BuiltInSessionStore(SessionChatMemoryUtils.createSessionRepository(dataSource));
    }

    private static BuiltInSessionStore createRedisSessionRepository(
        Environment environment, Function<Environment, RedisClient> redisClientFactory) {

        RedisClient redisClient = redisClientFactory.apply(environment);

        return new BuiltInSessionStore(
            createRedisSessionRepository(
                redisClient, environment.getRequiredProperty("bytechef.ai.memory.redis.session-key-prefix"),
                environment.getRequiredProperty(SESSION_MAX_ARCHIVED_EVENTS, Integer.class)),
            redisClient);
    }

    static SessionRepository createRedisSessionRepository(
        UnifiedJedis jedis, String keyPrefix, int maxArchivedEvents) {

        Objects.requireNonNull(keyPrefix, "keyPrefix must not be null");

        requireNonNegativeMaxArchivedEvents(maxArchivedEvents);

        return new TenantRoutingSessionRepository(tenantId -> RedisSessionRepository.builder()
            .jedis(jedis)
            .keyPrefix(keyPrefix + tenantId + ":")
            .maxArchivedEvents(maxArchivedEvents)
            .build());
    }

    private static BuiltInSessionStore createS3SessionRepository(
        Environment environment, Function<Environment, S3Client> s3ClientFactory) {

        S3Client s3Client = s3ClientFactory.apply(environment);

        String bucketPrefix = environment.getRequiredProperty(MEMORY_AWS_PREFIX + "session-bucket-prefix");
        String keyPrefix = environment.getRequiredProperty(MEMORY_AWS_PREFIX + "key-prefix");
        int maxArchivedEvents = environment.getRequiredProperty(SESSION_MAX_ARCHIVED_EVENTS, Integer.class);

        return new BuiltInSessionStore(
            createS3SessionRepository(s3Client, bucketPrefix, keyPrefix, maxArchivedEvents), s3Client);
    }

    static SessionRepository createS3SessionRepository(
        S3Client s3Client, String bucketPrefix, String keyPrefix, int maxArchivedEvents) {

        if (StringUtils.isBlank(bucketPrefix)) {
            throw new IllegalArgumentException(MEMORY_AWS_PREFIX + "session-bucket-prefix must not be blank");
        }

        Objects.requireNonNull(keyPrefix, "keyPrefix must not be null");

        requireNonNegativeMaxArchivedEvents(maxArchivedEvents);

        Cache<String, SessionRepository> repositories = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build();

        return new TenantRoutingSessionRepository(
            repositories.asMap(), tenantId -> new TenantBucketS3SessionRepository(
                s3Client, TenantBucketS3SessionRepository.toBucketName(bucketPrefix, tenantId), keyPrefix,
                maxArchivedEvents));
    }

    private static void requireNonNegativeMaxArchivedEvents(int maxArchivedEvents) {
        if (maxArchivedEvents < 0) {
            throw new IllegalArgumentException(
                SESSION_MAX_ARCHIVED_EVENTS + " must not be negative but was " + maxArchivedEvents);
        }
    }

    private static RedisClient buildRedisClient(Environment environment) {
        String host = environment.getRequiredProperty("bytechef.ai.memory.redis.host");
        int port = environment.getRequiredProperty("bytechef.ai.memory.redis.port", Integer.class);
        String username = environment.getProperty("bytechef.ai.memory.redis.username");
        String password = environment.getProperty("bytechef.ai.memory.redis.password");

        if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
            throw new IllegalArgumentException(
                "bytechef.ai.memory.redis.password is required when a username is configured");
        }

        if (StringUtils.isBlank(password)) {
            return RedisClient.create(host, port);
        }

        return RedisClient.create(host, port, StringUtils.trimToNull(username), password);
    }

    private static S3Client buildS3Client(Environment environment) {
        S3ClientBuilder builder = S3Client.builder();

        String region = StringUtils.firstNonBlank(
            environment.getProperty(MEMORY_AWS_PREFIX + "region"),
            environment.getProperty(CLOUD_AWS_PREFIX + "region"));

        if (region != null) {
            builder.region(Region.of(region));
        }

        ResolvedAwsCredentials resolvedAwsCredentials = resolveAwsCredentials(environment);

        log.info(
            "Built-in session chat memory S3 client uses AWS credentials from {} in region {}",
            resolvedAwsCredentials.source(), region == null ? "resolved by the default region provider chain" : region);

        return builder.credentialsProvider(resolvedAwsCredentials.awsCredentialsProvider())
            .build();
    }

    static AwsCredentialsProvider getAwsCredentialsProvider(Environment environment) {
        ResolvedAwsCredentials resolvedAwsCredentials = resolveAwsCredentials(environment);

        return resolvedAwsCredentials.awsCredentialsProvider();
    }

    private static ResolvedAwsCredentials resolveAwsCredentials(Environment environment) {
        AwsBasicCredentials memoryAwsCredentials = getAwsCredentials(environment, MEMORY_AWS_PREFIX);

        if (memoryAwsCredentials != null) {
            return new ResolvedAwsCredentials(
                StaticCredentialsProvider.create(memoryAwsCredentials), MEMORY_AWS_PREFIX + "*");
        }

        AwsBasicCredentials cloudAwsCredentials = getAwsCredentials(environment, CLOUD_AWS_PREFIX);

        if (cloudAwsCredentials != null) {
            return new ResolvedAwsCredentials(
                StaticCredentialsProvider.create(cloudAwsCredentials), CLOUD_AWS_PREFIX + "*");
        }

        return new ResolvedAwsCredentials(
            DefaultCredentialsProvider.builder()
                .build(),
            "the default credentials provider chain");
    }

    private static @Nullable AwsBasicCredentials getAwsCredentials(Environment environment, String propertyPrefix) {
        String accessKeyId = environment.getProperty(propertyPrefix + "access-key-id");
        String secretAccessKey = environment.getProperty(propertyPrefix + "secret-access-key");

        if (StringUtils.isAllBlank(accessKeyId, secretAccessKey)) {
            return null;
        }

        if (StringUtils.isAnyBlank(accessKeyId, secretAccessKey)) {
            throw new IllegalArgumentException(
                propertyPrefix + "access-key-id and " + propertyPrefix + "secret-access-key must be set together");
        }

        return AwsBasicCredentials.create(accessKeyId, secretAccessKey);
    }
}
