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
import static org.mockito.Mockito.mock;

import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class PgVectorSchemaUtilsIntTest {

    private static final int NODES = 6;
    private static final int ROUNDS = 5;

    @Container
    private static final PostgreSQLContainer<?> PGVECTOR_CONTAINER = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"));

    private static final List<HikariDataSource> NODE_DATA_SOURCES = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        for (int node = 0; node < NODES; node++) {
            HikariDataSource hikariDataSource = new HikariDataSource();

            hikariDataSource.setJdbcUrl(PGVECTOR_CONTAINER.getJdbcUrl());
            hikariDataSource.setUsername(PGVECTOR_CONTAINER.getUsername());
            hikariDataSource.setPassword(PGVECTOR_CONTAINER.getPassword());

            NODE_DATA_SOURCES.add(hikariDataSource);
        }
    }

    @AfterAll
    static void afterAll() {
        NODE_DATA_SOURCES.forEach(HikariDataSource::close);
    }

    @Test
    void testNodesStartingTogetherAllCreateTheSchema() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(NODE_DATA_SOURCES.getFirst());
        List<Throwable> failures = new ArrayList<>();

        try (ExecutorService executorService = Executors.newFixedThreadPool(NODES)) {
            for (int round = 0; round < ROUNDS; round++) {
                jdbcTemplate.execute("DROP TABLE IF EXISTS public.test_vector_store");
                jdbcTemplate.execute("DROP EXTENSION IF EXISTS vector CASCADE");
                jdbcTemplate.execute("DROP EXTENSION IF EXISTS hstore CASCADE");

                CyclicBarrier cyclicBarrier = new CyclicBarrier(NODES);
                List<CompletableFuture<Void>> startups = new ArrayList<>();

                for (HikariDataSource nodeDataSource : NODE_DATA_SOURCES) {
                    startups.add(CompletableFuture.runAsync(() -> {
                        PgVectorStore pgVectorStore = createPgVectorStore(new JdbcTemplate(nodeDataSource));

                        await(cyclicBarrier);

                        PgVectorSchemaUtils.initializeSchema(pgVectorStore::afterPropertiesSet);
                    }, executorService));
                }

                for (CompletableFuture<Void> startup : startups) {
                    try {
                        startup.join();
                    } catch (RuntimeException exception) {
                        failures.add(exception);
                    }
                }
            }
        }

        assertThat(failures).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'test_vector_store'",
            Integer.class)).isEqualTo(1);
    }

    private static void await(CyclicBarrier cyclicBarrier) {
        try {
            cyclicBarrier.await();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static PgVectorStore createPgVectorStore(JdbcTemplate jdbcTemplate) {
        return PgVectorStore.builder(jdbcTemplate, mock(EmbeddingModel.class))
            .vectorTableName("test_vector_store")
            .dimensions(3)
            .initializeSchema(true)
            .build();
    }
}
