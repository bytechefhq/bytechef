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

import com.bytechef.component.ai.agent.chat.memory.jdbc.session.util.SessionChatMemoryUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.redis.RedisSessionRepository;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Resolves the built-in {@link SessionRepository} from the application's {@code bytechef.ai.memory.provider}
 * configuration: {@code jdbc} (default — backed by the application database), {@code aws} (S3), {@code redis}, or
 * {@code in_memory}. When the provider is jdbc but no {@link DataSource} is available (lightweight app variants), the
 * repository degrades to in-memory so the component still starts.
 *
 * @author Ivica Cardic
 */
public final class BuiltInSessionRepositoryFactory {

    private BuiltInSessionRepositoryFactory() {
    }

    /**
     * The resolved repository plus the client that owns its connections, if any — the component handler closes it on
     * shutdown. {@code closeable} is {@code null} for the jdbc (pool owned by the application) and in-memory backends.
     */
    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public record BuiltInSessionRepository(SessionRepository sessionRepository, @Nullable AutoCloseable closeable) {
    }

    public static BuiltInSessionRepository create(Environment environment, @Nullable JdbcTemplate jdbcTemplate) {
        String provider = environment.getProperty("bytechef.ai.memory.provider", "jdbc");

        return switch (provider) {
            case "aws" -> createS3SessionRepository(environment);
            case "redis" -> createRedisSessionRepository(environment);
            case "in_memory" -> new BuiltInSessionRepository(
                InMemorySessionRepository.builder()
                    .build(),
                null);
            default -> createJdbcSessionRepository(jdbcTemplate);
        };
    }

    private static BuiltInSessionRepository createJdbcSessionRepository(@Nullable JdbcTemplate jdbcTemplate) {
        DataSource dataSource = jdbcTemplate == null ? null : jdbcTemplate.getDataSource();

        if (dataSource == null) {
            return new BuiltInSessionRepository(
                InMemorySessionRepository.builder()
                    .build(),
                null);
        }

        return new BuiltInSessionRepository(SessionChatMemoryUtils.getSessionRepository(dataSource), null);
    }

    private static BuiltInSessionRepository createRedisSessionRepository(Environment environment) {
        JedisPooled jedisPooled = buildJedisPooled(environment);

        return new BuiltInSessionRepository(
            RedisSessionRepository.builder()
                .jedis(jedisPooled)
                .keyPrefix(environment.getProperty("bytechef.ai.memory.redis.key-prefix", "bytechef-session:"))
                .build(),
            jedisPooled);
    }

    private static BuiltInSessionRepository createS3SessionRepository(Environment environment) {
        S3Client s3Client = buildS3Client(environment);

        String bucketPrefix = environment.getProperty("bytechef.ai.memory.aws.bucket-prefix", "bytechef-session");
        String keyPrefix = environment.getProperty("bytechef.ai.memory.aws.key-prefix", "");

        return new BuiltInSessionRepository(
            new TenantRoutingS3SessionRepository(s3Client, bucketPrefix, keyPrefix), s3Client);
    }

    private static JedisPooled buildJedisPooled(Environment environment) {
        String host = environment.getProperty("bytechef.ai.memory.redis.host", "localhost");
        int port = environment.getProperty("bytechef.ai.memory.redis.port", Integer.class, 6379);
        String username = environment.getProperty("bytechef.ai.memory.redis.username");
        String password = environment.getProperty("bytechef.ai.memory.redis.password");

        if (username != null && !username.isBlank() && (password == null || password.isBlank())) {
            throw new IllegalArgumentException(
                "bytechef.ai.memory.redis.password is required when a username is configured");
        }

        if (password == null || password.isBlank()) {
            return new JedisPooled(host, port);
        }

        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
            .password(password);

        if (username != null && !username.isBlank()) {
            clientConfigBuilder.user(username);
        }

        return new JedisPooled(new HostAndPort(host, port), clientConfigBuilder.build());
    }

    private static S3Client buildS3Client(Environment environment) {
        S3ClientBuilder builder = S3Client.builder();

        String region = environment.getProperty("bytechef.ai.memory.aws.region");

        if (region != null && !region.isBlank()) {
            builder.region(Region.of(region));
        }

        String accessKeyId = environment.getProperty("bytechef.ai.memory.aws.access-key-id");
        String secretAccessKey = environment.getProperty("bytechef.ai.memory.aws.secret-access-key");

        if (accessKeyId != null && !accessKeyId.isBlank() && secretAccessKey != null && !secretAccessKey.isBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
