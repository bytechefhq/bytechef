/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.contextstore.config.ContextStoreIntTestConfiguration;
import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link ContextStoreSourceService} against a real Postgres instance via Testcontainers. Verifies
 * the full create/fetch/list/update/delete round-trip with the auto-configured Spring Data JDBC repositories.
 *
 * <p>
 * Workspace-scoped reads now flow through the automation-side {@code WorkspaceContextStoreSourceService} (via the
 * relation table); this platform IntTest stays workspace-agnostic and uses the id-based bulk lookups directly.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = ContextStoreIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class ContextStoreSourceServiceIntTest {

    @Autowired
    private ContextStoreService contextStoreService;

    @Autowired
    private ContextStoreSourceService contextStoreSourceService;

    @AfterEach
    void cleanup() {
        contextStoreSourceService.findAllActiveAcrossWorkspaces()
            .forEach(source -> contextStoreSourceService.delete(source.getId()));
    }

    private Long givenContextStore() {
        ContextStore store = new ContextStore();

        store.setName("test-store-" + System.nanoTime());
        store.setEnvironment(Environment.DEVELOPMENT);

        return contextStoreService.create(store)
            .getId();
    }

    @Test
    void testCreateAndFetch() {
        ContextStoreSource source = newSource("HubSpot Production");

        ContextStoreSource created = contextStoreSourceService.create(source);

        assertThat(created.getId()).isNotNull();
        assertThat(contextStoreSourceService.fetch(created.getId())).isPresent();
        assertThat(contextStoreSourceService.get(created.getId())
            .getName())
                .isEqualTo("HubSpot Production");
    }

    @Test
    void testGetAllByIds() {
        ContextStoreSource hubspot = contextStoreSourceService.create(newSource("HubSpot Prod"));
        ContextStoreSource salesforce = contextStoreSourceService.create(newSource("Salesforce Prod"));

        List<ContextStoreSource> sources =
            contextStoreSourceService.getAllByIds(List.of(hubspot.getId(), salesforce.getId()));

        assertThat(sources).hasSize(2);
    }

    @Test
    void testGetAllEnabledByIdsSkipsDisabled() {
        ContextStoreSource enabledSource = contextStoreSourceService.create(newSource("Enabled HubSpot"));
        ContextStoreSource disabledSource = contextStoreSourceService.create(newSource("Disabled HubSpot"));

        contextStoreSourceService.setEnabled(disabledSource.getId(), false);

        List<ContextStoreSource> enabledSources =
            contextStoreSourceService.getAllEnabledByIds(List.of(enabledSource.getId(), disabledSource.getId()));

        assertThat(enabledSources)
            .hasSize(1)
            .extracting(ContextStoreSource::getId)
            .containsExactly(enabledSource.getId());
    }

    @Test
    void testUpdateStatusPersistsLifecycleFields() {
        ContextStoreSource created = contextStoreSourceService.create(newSource("HubSpot"));

        Instant lastSyncRunAt = Instant.parse("2026-05-08T12:00:00Z");

        contextStoreSourceService.updateStatus(created.getId(), ContextStoreSourceStatus.READY, lastSyncRunAt, 42L);

        ContextStoreSource refreshed = contextStoreSourceService.get(created.getId());

        assertThat(refreshed.getStatus()).isEqualTo(ContextStoreSourceStatus.READY);
        assertThat(refreshed.getLastSyncRunAt()).isEqualTo(lastSyncRunAt);
        assertThat(refreshed.getLastSyncJobExecutionId()).isEqualTo(42L);
    }

    @Test
    void testUpdateLastSyncMetadataDoesNotChangeStatus() {
        ContextStoreSource created = contextStoreSourceService.create(newSource("HubSpot"));

        // Source starts in BUILDING_PREVIEW per newSource(); a PARTIAL run must not promote it to READY.
        Instant lastSyncRunAt = Instant.parse("2026-05-09T08:30:00Z");

        contextStoreSourceService.updateLastSyncMetadata(created.getId(), lastSyncRunAt, 77L);

        ContextStoreSource refreshed = contextStoreSourceService.get(created.getId());

        assertThat(refreshed.getStatus()).isEqualTo(ContextStoreSourceStatus.BUILDING_PREVIEW);
        assertThat(refreshed.getLastSyncRunAt()).isEqualTo(lastSyncRunAt);
        assertThat(refreshed.getLastSyncJobExecutionId()).isEqualTo(77L);
    }

    @Test
    void testWorkflowIdRoundTrip() {
        ContextStoreSource source = newSource("HubSpot");

        ContextStoreSource created = contextStoreSourceService.create(source);

        created.setWorkflowId("wf-uuid-abc");

        contextStoreSourceService.update(created);

        ContextStoreSource refreshed = contextStoreSourceService.get(created.getId());

        assertThat(refreshed.getWorkflowId()).isEqualTo("wf-uuid-abc");
    }

    @Test
    void testGetThrowsWhenMissing() {
        assertThatThrownBy(() -> contextStoreSourceService.get(99_999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("99999");
    }

    private ContextStoreSource newSource(String name) {
        ContextStoreSource source = new ContextStoreSource();

        source.setContextStoreId(givenContextStore());
        source.setName(name);
        // Source absorbed the former Entity layer — entityName, idField, and indexedFields are required NOT NULL
        // columns on context_store_source. The values are test-shaped; the IT only exercises the source-row
        // lifecycle, not the record-shape semantics.
        source.setEntityName("contacts");
        source.setIdField("id");
        source.setIndexedFields(java.util.Map.of("email", "TEXT"));
        source.setSourceComponentName("hubspot");
        source.setSourceComponentVersion(1);
        source.setSourceClusterElementName("contactsReader");
        source.setCadence("@hourly");
        source.setStatus(ContextStoreSourceStatus.BUILDING_PREVIEW);

        return source;
    }
}
