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

package com.bytechef.platform.knowledgebase.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfiguration;
import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfigurationSharedMocks;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseSourceRepository;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Proves the orphan against a real Postgres: a source's {@code workflow_id} is a nullable column with no foreign key,
 * so nothing at the database level stops it outliving the workflow it names. The unit test mocks
 * {@code findAllByWorkflowId}; this one exercises the derived query itself, which is the half a mock cannot check.
 *
 * <p>
 * Each test asserts the pointer is set BEFORE the delete and repaired after — a silent orphan produces no exception, so
 * asserting only the end state would pass just as well against a listener that never ran.
 * </p>
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@KnowledgeBaseIntTestConfigurationSharedMocks
class KnowledgeBaseSourceWorkflowPreDeleteListenerIntTest {

    private static final String WORKFLOW_ID = "wf-1";
    private static final String OTHER_WORKFLOW_ID = "wf-2";

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private KnowledgeBaseSourceRepository knowledgeBaseSourceRepository;

    @Autowired
    private KnowledgeBaseSourceService knowledgeBaseSourceService;

    private KnowledgeBase knowledgeBase;
    private KnowledgeBaseSourceWorkflowPreDeleteListener listener;

    @BeforeEach
    void beforeEach() {
        knowledgeBaseSourceRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();

        knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Test KnowledgeBase");

        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);

        listener = new KnowledgeBaseSourceWorkflowPreDeleteListener(knowledgeBaseSourceService);
    }

    @AfterEach
    void afterEach() {
        knowledgeBaseSourceRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
    }

    @Test
    void testTheWorkflowPointerIsClearedAndTheSourceSurvives() {
        KnowledgeBaseSource source = knowledgeBaseSourceService.create(newSource("HubSpot Production", WORKFLOW_ID));

        Long sourceId = source.getId();

        // Before: the row exists and names the workflow.
        assertThat(knowledgeBaseSourceService.get(sourceId)
            .getWorkflowId()).isEqualTo(WORKFLOW_ID);

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        // After: the pointer is gone, the source is not.
        assertThat(knowledgeBaseSourceService.fetch(sourceId)).isPresent();
        assertThat(knowledgeBaseSourceService.get(sourceId)
            .getWorkflowId()).isNull();
        assertThat(knowledgeBaseSourceService.get(sourceId)
            .getName()).isEqualTo("HubSpot Production");
    }

    @Test
    void testOnlySourcesNamingTheDeletedWorkflowAreTouched() {
        KnowledgeBaseSource deleted = knowledgeBaseSourceService.create(newSource("Deleted", WORKFLOW_ID));
        KnowledgeBaseSource kept = knowledgeBaseSourceService.create(newSource("Kept", OTHER_WORKFLOW_ID));

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        assertThat(knowledgeBaseSourceService.get(deleted.getId())
            .getWorkflowId()).isNull();
        assertThat(knowledgeBaseSourceService.get(kept.getId())
            .getWorkflowId()).isEqualTo(OTHER_WORKFLOW_ID);
    }

    /**
     * The derived query has to return an empty list rather than every row with a null {@code workflow_id}, which is
     * what a mock-based test cannot tell you.
     */
    @Test
    void testAWorkflowNoSourceNamesLeavesEverythingAlone() {
        KnowledgeBaseSource source = knowledgeBaseSourceService.create(newSource("Untouched", OTHER_WORKFLOW_ID));
        KnowledgeBaseSource unbound = knowledgeBaseSourceService.create(newSource("Unbound", null));

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        assertThat(knowledgeBaseSourceService.get(source.getId())
            .getWorkflowId()).isEqualTo(OTHER_WORKFLOW_ID);
        assertThat(knowledgeBaseSourceService.get(unbound.getId())
            .getWorkflowId()).isNull();
    }

    private KnowledgeBaseSource newSource(String name, String workflowId) {
        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setName(name);
        source.setSourceComponentName("hubspot");
        source.setSourceComponentVersion(1);
        source.setSourceClusterElementName("contactsReader");
        source.setKnowledgeBaseId(knowledgeBase.getId());
        source.setCadence("@hourly");
        source.setStatus(KnowledgeBaseSourceStatus.BUILDING_PREVIEW);
        source.setWorkflowId(workflowId);

        return source;
    }
}
