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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Objects;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class WorkspaceApiKeyServiceIntTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private WorkspaceApiKeyRepository workspaceApiKeyRepository;

    @Autowired
    private WorkspaceApiKeyService workspaceApiKeyService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long apiKeyId;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test"));

        workspaceId = workspace.getId();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Long insertedApiKeyId = Objects.requireNonNull(jdbcTemplate.queryForObject(
            "INSERT INTO api_key (name, secret_key, user_id, environment, created_date, created_by, " +
                "last_modified_date, last_modified_by) VALUES ('test', 'secret', 1, 0, now(), 'test', now(), 'test') " +
                "RETURNING id",
            Long.class));

        apiKeyId = insertedApiKeyId;
    }

    @AfterEach
    void afterEach() {
        workspaceApiKeyRepository.deleteAll();
        workspaceRepository.deleteAll();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM api_key");
    }

    @Test
    void testCreate() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyService.create(apiKeyId, workspaceId);

        assertThat(workspaceApiKeyRepository.findById(workspaceApiKey.getId()))
            .hasValueSatisfying(savedWorkspaceApiKey -> {
                assertThat(savedWorkspaceApiKey.getApiKeyId()).isEqualTo(apiKeyId);
                assertThat(savedWorkspaceApiKey.getWorkspaceId()).isEqualTo(workspaceId);
            });
    }

    @Test
    void testCreateInRolledBackTransactionPersistsNothing() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            workspaceApiKeyService.create(apiKeyId, workspaceId);

            transactionStatus.setRollbackOnly();
        });

        assertThat(workspaceApiKeyRepository.findAll()).isEmpty();
    }

    @Test
    void testDelete() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKeyId, workspaceId));

        workspaceApiKeyService.delete(workspaceApiKey.getId());

        assertThat(workspaceApiKeyRepository.findById(workspaceApiKey.getId())).isEmpty();
    }

    @Test
    void testDeleteOfMissingWorkspaceApiKeyIsNoOp() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKeyId, workspaceId));

        workspaceApiKeyService.delete(Long.MAX_VALUE);

        assertThat(workspaceApiKeyRepository.findById(workspaceApiKey.getId())).isPresent();
    }

    @Test
    void testDeleteWorkspaceApiKeyDeletesBinding() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKeyId, workspaceId));

        workspaceApiKeyService.deleteWorkspaceApiKey(apiKeyId);

        assertThat(workspaceApiKeyRepository.findById(workspaceApiKey.getId())).isEmpty();
    }
}
