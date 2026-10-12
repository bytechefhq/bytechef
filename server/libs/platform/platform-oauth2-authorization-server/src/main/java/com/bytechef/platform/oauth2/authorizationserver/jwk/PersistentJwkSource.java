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

import com.bytechef.encryption.Encryption;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantScope;
import com.bytechef.tenant.TenantContext;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;

/**
 * Serves the authorization server's RSA signing key from the shared {@code oauth2_jwk} table, creating it on first use,
 * so that every node and every restart signs and verifies tokens with the same key.
 *
 * @author Ivica Cardic
 */
public class PersistentJwkSource implements JWKSource<SecurityContext> {

    static final long JWK_CREATION_LOCK_KEY = 3_213_000_002L;

    private static final String INSERT_JWK_SQL =
        "INSERT INTO oauth2_jwk (id, private_key) VALUES (?, ?) ON CONFLICT (id) DO NOTHING";
    private static final String LOCK_SQL = "SELECT pg_advisory_xact_lock(?)";
    private static final String SELECT_OLDEST_JWK_SQL =
        "SELECT private_key FROM oauth2_jwk ORDER BY created_date, id LIMIT 1";

    private final DataSource dataSource;
    private final Encryption encryption;
    private volatile @Nullable JWKSet jwkSet;

    @SuppressFBWarnings("EI2")
    public PersistentJwkSource(DataSource dataSource, Encryption encryption) {
        this.dataSource = dataSource;
        this.encryption = encryption;
    }

    @Override
    public List<JWK> get(JWKSelector jwkSelector, @Nullable SecurityContext context) {
        return jwkSelector.select(getJwkSet());
    }

    private JWKSet getJwkSet() {
        JWKSet currentJwkSet = jwkSet;

        if (currentJwkSet == null) {
            synchronized (this) {
                currentJwkSet = jwkSet;

                if (currentJwkSet == null) {
                    currentJwkSet = TenantScope.call(TenantContext.DEFAULT_TENANT_ID, this::loadOrCreateJwkSet);

                    jwkSet = currentJwkSet;
                }
            }
        }

        return currentJwkSet;
    }

    private JWKSet loadOrCreateJwkSet() {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();

            connection.setAutoCommit(false);

            JWKSet loadedJwkSet;

            try {
                loadedJwkSet = loadOrCreateJwkSet(connection);

                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                restoreAutoCommit(connection, autoCommit, exception);

                throw exception;
            }

            connection.setAutoCommit(autoCommit);

            return loadedJwkSet;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load the OAuth2 authorization server signing key", exception);
        }
    }

    private JWKSet loadOrCreateJwkSet(Connection connection) throws SQLException {
        try (PreparedStatement lockStatement = connection.prepareStatement(LOCK_SQL)) {
            lockStatement.setLong(1, JWK_CREATION_LOCK_KEY);

            lockStatement.execute();
        }

        RSAKey rsaKey = selectOldestRsaKey(connection);

        if (rsaKey == null) {
            insertRsaKey(connection, generateRsaKey());

            rsaKey = selectOldestRsaKey(connection);
        }

        if (rsaKey == null) {
            throw new IllegalStateException("The OAuth2 authorization server signing key was not persisted");
        }

        return new JWKSet(rsaKey);
    }

    private void insertRsaKey(Connection connection, RSAKey rsaKey) throws SQLException {
        try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_JWK_SQL)) {
            insertStatement.setString(1, rsaKey.getKeyID());
            insertStatement.setString(2, encryption.encrypt(rsaKey.toJSONString()));

            insertStatement.executeUpdate();
        }
    }

    private @Nullable RSAKey selectOldestRsaKey(Connection connection) throws SQLException {
        try (PreparedStatement selectStatement = connection.prepareStatement(SELECT_OLDEST_JWK_SQL);
            ResultSet resultSet = selectStatement.executeQuery()) {

            if (!resultSet.next()) {
                return null;
            }

            return parseRsaKey(encryption.decrypt(resultSet.getString(1)));
        }
    }

    private static void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private static void restoreAutoCommit(Connection connection, boolean autoCommit, Exception originalException) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException autoCommitException) {
            originalException.addSuppressed(autoCommitException);
        }
    }

    private static RSAKey parseRsaKey(String json) {
        try {
            return RSAKey.parse(json);
        } catch (ParseException parseException) {
            throw new IllegalStateException(
                "Unable to parse the OAuth2 authorization server signing key", parseException);
        }
    }

    private static RSAKey generateRsaKey() {
        KeyPair keyPair = generateRsaKeyPair();

        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey((RSAPrivateKey) keyPair.getPrivate())
            .keyID(UUID.randomUUID()
                .toString())
            .build();
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to generate RSA key pair", exception);
        }
    }
}
