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

package com.bytechef.automation.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.knowledgebase.config.AutomationKnowledgeBaseIntTestConfiguration;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseSourceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test for {@link WorkspaceKnowledgeBaseSourceService}. Verifies the workspace-scoped lookups over the
 * nullable {@code knowledge_base_source.workspace_id} column, delegating to the platform-side
 * {@code KnowledgeBaseSourceService}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationKnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class WorkspaceKnowledgeBaseSourceServiceIntTest {

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private KnowledgeBaseSourceRepository knowledgeBaseSourceRepository;

    @Autowired
    private WorkspaceKnowledgeBaseSourceService workspaceKnowledgeBaseSourceService;

    private KnowledgeBase knowledgeBase;

    @BeforeEach
    void beforeEach() {
        knowledgeBaseSourceRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();

        knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Test KnowledgeBase");

        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
    }

    @AfterEach
    void afterEach() {
        knowledgeBaseSourceRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
    }

    @Test
    void testFetchWorkspaceIdByKnowledgeBaseSourceIdReturnsCorrectWorkspace() {
        KnowledgeBaseSource source = persistSource("HubSpot", 42L);

        Optional<Long> workspaceId =
            workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(source.getId());

        assertThat(workspaceId).hasValue(42L);
    }

    @Test
    void testFetchWorkspaceIdReturnsEmptyForOrphanSource() {
        KnowledgeBaseSource source = persistSource("Orphan", null);

        Optional<Long> workspaceId =
            workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(source.getId());

        assertThat(workspaceId).isEmpty();
    }

    @Test
    void testGetAllSourcesByWorkspaceIdIsScopedToThatWorkspace() {
        persistSource("HubSpot", 1L);
        persistSource("Salesforce", 1L);
        persistSource("Other", 2L);

        List<KnowledgeBaseSource> sources = workspaceKnowledgeBaseSourceService.getAllSourcesByWorkspaceId(1L);

        assertThat(sources)
            .hasSize(2)
            .extracting(KnowledgeBaseSource::getName)
            .containsExactlyInAnyOrder("HubSpot", "Salesforce");
    }

    @Test
    void testGetAllSourcesByWorkspaceIdIgnoresWorkspaceLessSources() {
        persistSource("HubSpot", 1L);
        persistSource("Orphan", null);

        List<KnowledgeBaseSource> sources = workspaceKnowledgeBaseSourceService.getAllSourcesByWorkspaceId(1L);

        assertThat(sources)
            .extracting(KnowledgeBaseSource::getName)
            .containsExactly("HubSpot");
    }

    @Test
    void testGetAllEnabledSourcesByWorkspaceIdFiltersDisabled() {
        persistSource("Enabled", 1L);

        KnowledgeBaseSource disabled = persistSource("Disabled", 1L);

        disabled.setEnabled(false);
        knowledgeBaseSourceRepository.save(disabled);

        List<KnowledgeBaseSource> sources = workspaceKnowledgeBaseSourceService.getAllEnabledSourcesByWorkspaceId(1L);

        assertThat(sources)
            .hasSize(1)
            .extracting(KnowledgeBaseSource::getName)
            .containsExactly("Enabled");
    }

    private KnowledgeBaseSource persistSource(String name, @Nullable Long workspaceId) {
        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setName(name);
        source.setSourceComponentName("hubspot");
        source.setSourceComponentVersion(1);
        source.setSourceClusterElementName("contactsReader");
        source.setKnowledgeBaseId(knowledgeBase.getId());
        source.setCadence("@hourly");
        source.setStatus(KnowledgeBaseSourceStatus.BUILDING_PREVIEW);
        source.setWorkspaceId(workspaceId);

        return knowledgeBaseSourceRepository.save(source);
    }
}
