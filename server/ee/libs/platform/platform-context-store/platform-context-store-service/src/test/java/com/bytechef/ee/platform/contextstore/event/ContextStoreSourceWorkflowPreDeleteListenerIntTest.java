/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.contextstore.config.ContextStoreIntTestConfiguration;
import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.service.ContextStoreService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves the orphan against a real Postgres, and is the context-store twin of
 * {@code KnowledgeBaseSourceWorkflowPreDeleteListenerIntTest}: {@code context_store_source.workflow_id} is a nullable
 * column with no foreign key, so nothing at the database level stops a source outliving the workflow it names. The unit
 * test mocks {@code findAllByWorkflowId}; this one exercises the derived query itself.
 *
 * <p>
 * Each test asserts the pointer is set BEFORE the delete and repaired after — a silent orphan raises nothing, so
 * asserting only the end state would pass against a listener that never ran.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = ContextStoreIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class ContextStoreSourceWorkflowPreDeleteListenerIntTest {

    private static final String WORKFLOW_ID = "wf-1";
    private static final String OTHER_WORKFLOW_ID = "wf-2";

    @Autowired
    private ContextStoreService contextStoreService;

    @Autowired
    private ContextStoreSourceService contextStoreSourceService;

    private ContextStoreSourceWorkflowPreDeleteListener listener;

    @BeforeEach
    void beforeEach() {
        listener = new ContextStoreSourceWorkflowPreDeleteListener(contextStoreSourceService);
    }

    @AfterEach
    void cleanup() {
        contextStoreSourceService.findAllActiveAcrossWorkspaces()
            .forEach(source -> contextStoreSourceService.delete(source.getId()));
    }

    @Test
    void testTheWorkflowPointerIsClearedAndTheSourceSurvives() {
        ContextStoreSource source = contextStoreSourceService.create(newSource("HubSpot Production", WORKFLOW_ID));

        Long sourceId = source.getId();

        // Before: the row exists and names the workflow.
        assertThat(contextStoreSourceService.get(sourceId)
            .getWorkflowId()).isEqualTo(WORKFLOW_ID);

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        // After: the pointer is gone, the source is not.
        assertThat(contextStoreSourceService.fetch(sourceId)).isPresent();
        assertThat(contextStoreSourceService.get(sourceId)
            .getWorkflowId()).isNull();
        assertThat(contextStoreSourceService.get(sourceId)
            .getName()).isEqualTo("HubSpot Production");
    }

    @Test
    void testOnlySourcesNamingTheDeletedWorkflowAreTouched() {
        ContextStoreSource deleted = contextStoreSourceService.create(newSource("Deleted", WORKFLOW_ID));
        ContextStoreSource kept = contextStoreSourceService.create(newSource("Kept", OTHER_WORKFLOW_ID));

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        assertThat(contextStoreSourceService.get(deleted.getId())
            .getWorkflowId()).isNull();
        assertThat(contextStoreSourceService.get(kept.getId())
            .getWorkflowId()).isEqualTo(OTHER_WORKFLOW_ID);
    }

    /**
     * The derived query must return an empty list rather than every row whose {@code workflow_id} is null — the case a
     * mock-based test cannot distinguish.
     */
    @Test
    void testAWorkflowNoSourceNamesLeavesEverythingAlone() {
        ContextStoreSource source = contextStoreSourceService.create(newSource("Untouched", OTHER_WORKFLOW_ID));
        ContextStoreSource unbound = contextStoreSourceService.create(newSource("Unbound", null));

        listener.onWorkflowPreDelete(WORKFLOW_ID);

        assertThat(contextStoreSourceService.get(source.getId())
            .getWorkflowId()).isEqualTo(OTHER_WORKFLOW_ID);
        assertThat(contextStoreSourceService.get(unbound.getId())
            .getWorkflowId()).isNull();
    }

    private ContextStoreSource newSource(String name, String workflowId) {
        ContextStoreSource source = new ContextStoreSource();

        source.setContextStoreId(givenContextStore());
        source.setName(name);
        source.setEntityName("contacts");
        source.setIdField("id");
        source.setIndexedFields(Map.of("email", "TEXT"));
        source.setSourceComponentName("hubspot");
        source.setSourceComponentVersion(1);
        source.setSourceClusterElementName("contactsReader");
        source.setCadence("@hourly");
        source.setStatus(ContextStoreSourceStatus.BUILDING_PREVIEW);
        source.setWorkflowId(workflowId);

        return source;
    }

    private Long givenContextStore() {
        ContextStore store = new ContextStore();

        store.setName("test-store-" + System.nanoTime());
        store.setEnvironment(Environment.DEVELOPMENT);

        return contextStoreService.create(store)
            .getId();
    }
}
