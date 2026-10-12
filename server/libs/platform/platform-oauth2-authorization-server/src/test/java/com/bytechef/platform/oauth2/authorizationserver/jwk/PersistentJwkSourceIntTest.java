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

package com.bytechef.platform.oauth2.authorizationserver.jwk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.encryption.Encryption;
import com.bytechef.platform.oauth2.authorizationserver.config.Oauth2AuthorizationServerIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = Oauth2AuthorizationServerIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class PersistentJwkSourceIntTest {

    private static final JWKSelector ALL_KEYS_SELECTOR = new JWKSelector(new JWKMatcher.Builder().build());

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Encryption encryption;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM oauth2_jwk");
    }

    @Test
    void testFirstUseCreatesExactlyOneEncryptedKey() throws KeySourceException {
        PersistentJwkSource persistentJwkSource = new PersistentJwkSource(dataSource, encryption);

        List<JWK> jwks = persistentJwkSource.get(ALL_KEYS_SELECTOR, null);

        assertThat(jwks).hasSize(1);

        RSAKey rsaKey = (RSAKey) jwks.getFirst();

        assertThat(rsaKey.isPrivate()).isTrue();
        assertThat(countRows()).isEqualTo(1);

        String storedPrivateKey = jdbcTemplate.queryForObject(
            "SELECT private_key FROM oauth2_jwk WHERE id = ?", String.class, rsaKey.getKeyID());

        assertThat(storedPrivateKey)
            .isNotEqualTo(rsaKey.toJSONString())
            .doesNotContain("\"d\"")
            .doesNotContain(rsaKey.getPrivateExponent()
                .toString());
        assertThat(persistentJwkSource.get(ALL_KEYS_SELECTOR, null)).containsExactly(rsaKey);
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void testRestartReusesThePersistedKey() throws KeySourceException {
        RSAKey firstRsaKey = getSingleKey(new PersistentJwkSource(dataSource, encryption));
        RSAKey secondRsaKey = getSingleKey(new PersistentJwkSource(dataSource, encryption));

        assertThat(secondRsaKey.getKeyID()).isEqualTo(firstRsaKey.getKeyID());
        assertThat(secondRsaKey.toPublicJWK()).isEqualTo(firstRsaKey.toPublicJWK());
        assertThat(secondRsaKey.isPrivate()).isTrue();
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void testConcurrentFirstUseWaitsForTheCreationLockAndConvergesOnOneKey() throws Exception {
        PersistentJwkSource firstNodeJwkSource = new PersistentJwkSource(dataSource, encryption);
        PersistentJwkSource secondNodeJwkSource = new PersistentJwkSource(dataSource, encryption);

        try (ExecutorService executorService = Executors.newFixedThreadPool(2);
            Connection lockConnection = dataSource.getConnection()) {

            executeLockFunction(lockConnection, "SELECT pg_advisory_lock(?)");

            Future<RSAKey> firstNodeFuture;
            Future<RSAKey> secondNodeFuture;

            try {
                firstNodeFuture = executorService.submit(() -> getSingleKey(firstNodeJwkSource));
                secondNodeFuture = executorService.submit(() -> getSingleKey(secondNodeJwkSource));

                assertThatThrownBy(() -> firstNodeFuture.get(500, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
                assertThat(secondNodeFuture.isDone()).isFalse();
                assertThat(countRows()).isZero();
            } finally {
                executeLockFunction(lockConnection, "SELECT pg_advisory_unlock(?)");
            }

            RSAKey firstNodeRsaKey = firstNodeFuture.get(60, TimeUnit.SECONDS);
            RSAKey secondNodeRsaKey = secondNodeFuture.get(60, TimeUnit.SECONDS);

            assertThat(secondNodeRsaKey.getKeyID()).isEqualTo(firstNodeRsaKey.getKeyID());
            assertThat(secondNodeRsaKey.toPublicJWK()).isEqualTo(firstNodeRsaKey.toPublicJWK());
        }

        assertThat(countRows()).isEqualTo(1);
    }

    private static void executeLockFunction(Connection connection, String sql) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, PersistentJwkSource.JWK_CREATION_LOCK_KEY);

            preparedStatement.execute();
        }
    }

    private static RSAKey getSingleKey(PersistentJwkSource persistentJwkSource) throws KeySourceException {
        List<JWK> jwks = persistentJwkSource.get(ALL_KEYS_SELECTOR, null);

        assertThat(jwks).hasSize(1);

        return (RSAKey) jwks.getFirst();
    }

    private Integer countRows() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oauth2_jwk", Integer.class);
    }
}
