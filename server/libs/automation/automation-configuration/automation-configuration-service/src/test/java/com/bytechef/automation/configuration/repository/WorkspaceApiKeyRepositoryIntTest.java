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

package com.bytechef.automation.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class WorkspaceApiKeyRepositoryIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkspaceApiKeyRepository workspaceApiKeyRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @AfterEach
    public void afterEach() {
        workspaceApiKeyRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM api_key");
        workspaceRepository.deleteAll();
    }

    @Test
    public void testDeletingAnApiKeyRemovesItsWorkspaceLink() {
        Workspace workspace = workspaceRepository.save(new Workspace("api-key-owner"));

        Long apiKeyId = jdbcTemplate.queryForObject(
            "INSERT INTO api_key (name, secret_key, user_id, type, environment, created_date, created_by, " +
                "last_modified_date, last_modified_by) " +
                "VALUES ('departing user key', 'secret', 1, 0, 0, now(), 'test', now(), 'test') RETURNING id",
            Long.class);

        WorkspaceApiKey workspaceApiKey = workspaceApiKeyRepository.save(
            new WorkspaceApiKey(apiKeyId, workspace.getId()));

        jdbcTemplate.update("DELETE FROM api_key WHERE id = ?", apiKeyId);

        assertThat(workspaceApiKeyRepository.findById(workspaceApiKey.getId())).isEmpty();
    }
}
