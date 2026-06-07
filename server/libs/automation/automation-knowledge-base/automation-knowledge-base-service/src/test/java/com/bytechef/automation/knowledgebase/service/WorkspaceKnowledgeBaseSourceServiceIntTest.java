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
import com.bytechef.automation.knowledgebase.repository.WorkspaceKnowledgeBaseSourceRepository;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseSourceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test for {@link WorkspaceKnowledgeBaseSourceService}. Verifies the relation-table CRUD + workspace-scoped
 * lookups that join through to the platform-side {@code KnowledgeBaseSourceService}.
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
    private WorkspaceKnowledgeBaseSourceRepository workspaceKnowledgeBaseSourceRepository;

    @Autowired
    private WorkspaceKnowledgeBaseSourceService workspaceKnowledgeBaseSourceService;

    private KnowledgeBase knowledgeBase;

    @BeforeEach
    void beforeEach() {
        workspaceKnowledgeBaseSourceRepository.deleteAll();
        knowledgeBaseSourceRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();

        knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Test KnowledgeBase");

        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
    }

    @AfterEach
    void afterEach() {
        workspaceKnowledgeBaseSourceRepository.deleteAll();
        knowledgeBaseSourceRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
    }

    @Test
    void testRegisterSourceForWorkspaceInsertsRow() {
        KnowledgeBaseSource source = persistSource("HubSpot");

        workspaceKnowledgeBaseSourceService.registerSourceForWorkspace(source.getId(), 7L);

        assertThat(workspaceKnowledgeBaseSourceRepository.findByKnowledgeBaseSourceId(source.getId()))
            .isPresent()
            .map(relation -> relation.getWorkspaceId())
            .hasValue(7L);
    }

    @Test
    void testUnregisterSourceDeletesRow() {
        KnowledgeBaseSource source = persistSource("HubSpot");

        workspaceKnowledgeBaseSourceService.registerSourceForWorkspace(source.getId(), 7L);

        assertThat(workspaceKnowledgeBaseSourceRepository.findByKnowledgeBaseSourceId(source.getId())).isPresent();

        workspaceKnowledgeBaseSourceService.unregisterSource(source.getId());

        assertThat(workspaceKnowledgeBaseSourceRepository.findByKnowledgeBaseSourceId(source.getId())).isEmpty();
    }

    @Test
    void testFetchWorkspaceIdByKnowledgeBaseSourceIdReturnsCorrectWorkspace() {
        KnowledgeBaseSource source = persistSource("HubSpot");

        workspaceKnowledgeBaseSourceService.registerSourceForWorkspace(source.getId(), 42L);

        Optional<Long> workspaceId =
            workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(source.getId());

        assertThat(workspaceId).hasValue(42L);
    }

    @Test
    void testFetchWorkspaceIdReturnsEmptyForOrphanSource() {
        KnowledgeBaseSource source = persistSource("Orphan");

        Optional<Long> workspaceId =
            workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(source.getId());

        assertThat(workspaceId).isEmpty();
    }

    @Test
    void testGetAllSourcesByWorkspaceIdJoinsThroughToPlatformSources() {
        KnowledgeBaseSource hubspot = persistSource("HubSpot");
        KnowledgeBaseSource salesforce = persistSource("Salesforce");
        KnowledgeBaseSource otherWorkspaceSource = persistSource("Other");

        workspaceKnowledgeBaseSourceService.registerSourceForWorkspace(hubspot.getId(), 1L);
        workspaceKnowledgeBaseSourceService.registerSourceForWorkspace(salesforce.getId(), 1L);
        workspaceKnowledgeBaseSourceService.registerSourceForWorkspace(otherWorkspaceSource.getId(), 2L);

        List<KnowledgeBaseSource> sources = workspaceKnowledgeBaseSourceService.getAllSourcesByWorkspaceId(1L);

        assertThat(sources)
            .hasSize(2)
            .extracting(KnowledgeBaseSource::getName)
            .containsExactlyInAnyOrder("HubSpot", "Salesforce");
    }

    @Test
    void testGetAllEnabledSourcesByWorkspaceIdFiltersDisabled() {
        KnowledgeBaseSource enabled = persistSource("Enabled");
        KnowledgeBaseSource disabled = persistSource("Disabled");

        disabled.setEnabled(false);
        knowledgeBaseSourceRepository.save(disabled);

        workspaceKnowledgeBaseSourceService.registerSourceForWorkspace(enabled.getId(), 1L);
        workspaceKnowledgeBaseSourceService.registerSourceForWorkspace(disabled.getId(), 1L);

        List<KnowledgeBaseSource> sources = workspaceKnowledgeBaseSourceService.getAllEnabledSourcesByWorkspaceId(1L);

        assertThat(sources)
            .hasSize(1)
            .extracting(KnowledgeBaseSource::getName)
            .containsExactly("Enabled");
    }

    private KnowledgeBaseSource persistSource(String name) {
        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setName(name);
        source.setSourceComponentName("hubspot");
        source.setSourceComponentVersion(1);
        source.setSourceClusterElementName("contactsReader");
        source.setKnowledgeBaseId(knowledgeBase.getId());
        source.setCadence("@hourly");
        source.setStatus(KnowledgeBaseSourceStatus.BUILDING_PREVIEW);

        return knowledgeBaseSourceRepository.save(source);
    }
}
