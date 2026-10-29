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

package com.bytechef.pgvector.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.BadSqlGrammarException;

/**
 * @author Ivica Cardic
 */
class PgVectorSchemaUtilsTest {

    @Test
    void testInitializesAgainWhenAnotherNodeCreatedTheSameObject() {
        for (String sqlState : new String[] {
            "23505", "42P07", "42710"
        }) {
            AtomicInteger attempts = new AtomicInteger();

            PgVectorSchemaUtils.initializeSchema(() -> {
                if (attempts.incrementAndGet() == 1) {
                    throw new DuplicateKeyException("concurrent create", new SQLException("exists", sqlState));
                }
            });

            assertThat(attempts).as(sqlState)
                .hasValue(2);
        }
    }

    @Test
    void testInitializesAgainWhenTheConcurrentCreationIsWrapped() {
        AtomicInteger attempts = new AtomicInteger();

        PgVectorSchemaUtils.initializeSchema(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new RuntimeException(
                    "Unable to execute run with tenant ID 000001",
                    new DuplicateKeyException("concurrent create", new SQLException("exists", "23505")));
            }
        });

        assertThat(attempts).hasValue(2);
    }

    @Test
    void testRethrowsOtherFailures() {
        AtomicInteger attempts = new AtomicInteger();
        BadSqlGrammarException badSqlGrammarException = new BadSqlGrammarException(
            "create", "CREATE TABLE", new SQLException("permission denied", "42501"));

        assertThatThrownBy(() -> PgVectorSchemaUtils.initializeSchema(() -> {
            attempts.incrementAndGet();

            throw badSqlGrammarException;
        })).isSameAs(badSqlGrammarException);

        assertThat(attempts).hasValue(1);
    }

    @Test
    void testRethrowsAfterTheLastAttempt() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> PgVectorSchemaUtils.initializeSchema(() -> {
            attempts.incrementAndGet();

            throw new DataIntegrityViolationException(
                "concurrent create", new SQLException("duplicate key value", "23505"));
        })).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(attempts).hasValue(PgVectorSchemaUtils.MAX_ATTEMPTS);
    }
}
