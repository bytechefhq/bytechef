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

package com.bytechef.platform.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfiguration;
import com.bytechef.platform.knowledgebase.config.KnowledgeBaseIntTestConfigurationSharedMocks;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseSourceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test for {@link KnowledgeBaseSourceService} against a real Postgres instance via Testcontainers. The
 * platform-side service applies no workspace rules of its own; workspace-scoped reads flow through the automation-side
 * {@code WorkspaceKnowledgeBaseSourceService}, which filters on the nullable {@code knowledge_base_source.workspace_id}
 * column.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = KnowledgeBaseIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@KnowledgeBaseIntTestConfigurationSharedMocks
class KnowledgeBaseSourceServiceIntTest {

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private KnowledgeBaseSourceRepository knowledgeBaseSourceRepository;

    @Autowired
    private KnowledgeBaseSourceService knowledgeBaseSourceService;

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
    void testCreatePersistsAllFields() {
        KnowledgeBaseSource source = newSource("HubSpot Production");

        KnowledgeBaseSource created = knowledgeBaseSourceService.create(source);

        assertThat(created.getId()).isNotNull();
        assertThat(knowledgeBaseSourceService.fetch(created.getId())).isPresent();
        assertThat(knowledgeBaseSourceService.get(created.getId())
            .getName())
                .isEqualTo("HubSpot Production");
        assertThat(created.getStatus()).isEqualTo(KnowledgeBaseSourceStatus.BUILDING_PREVIEW);
        assertThat(created.isEnabled()).isTrue();
    }

    @Test
    void testUpdateBumpsVersion() {
        KnowledgeBaseSource created = knowledgeBaseSourceService.create(newSource("HubSpot"));
        int versionBefore = created.getVersion();

        created.setName("HubSpot Renamed");

        KnowledgeBaseSource updated = knowledgeBaseSourceService.update(created);

        assertThat(updated.getName()).isEqualTo("HubSpot Renamed");
        assertThat(updated.getVersion()).isGreaterThan(versionBefore);
    }

    @Test
    void testFetchByIdReturnsEmptyForUnknown() {
        assertThat(knowledgeBaseSourceService.fetch(999_999L)).isEmpty();
    }

    @Test
    void testGetThrowsWhenMissing() {
        assertThatThrownBy(() -> knowledgeBaseSourceService.get(999_999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("999999");
    }

    @Test
    void testFindAllByKnowledgeBaseId() {
        knowledgeBaseSourceService.create(newSource("HubSpot"));
        knowledgeBaseSourceService.create(newSource("Salesforce"));

        KnowledgeBase otherKb = new KnowledgeBase();

        otherKb.setName("Other KnowledgeBase");

        otherKb = knowledgeBaseRepository.save(otherKb);

        KnowledgeBaseSource otherSource = newSource("Stripe");

        otherSource.setKnowledgeBaseId(otherKb.getId());

        knowledgeBaseSourceService.create(otherSource);

        List<KnowledgeBaseSource> sources = knowledgeBaseSourceService.findAllByKnowledgeBaseId(knowledgeBase.getId());

        assertThat(sources).hasSize(2);
        assertThat(sources).extracting(KnowledgeBaseSource::getName)
            .containsExactlyInAnyOrder("HubSpot", "Salesforce");
    }

    @Test
    void testFindAllActiveAcrossKnowledgeBases() {
        KnowledgeBaseSource enabled = knowledgeBaseSourceService.create(newSource("Enabled"));
        KnowledgeBaseSource disabled = knowledgeBaseSourceService.create(newSource("Disabled"));

        knowledgeBaseSourceService.setEnabled(disabled.getId(), false);

        List<KnowledgeBaseSource> active = knowledgeBaseSourceService.findAllActiveAcrossKnowledgeBases();

        assertThat(active)
            .hasSize(1)
            .extracting(KnowledgeBaseSource::getId)
            .containsExactly(enabled.getId());
    }

    @Test
    void testGetAllByIdsReturnsRequestedSources() {
        KnowledgeBaseSource hubspot = knowledgeBaseSourceService.create(newSource("HubSpot"));
        KnowledgeBaseSource salesforce = knowledgeBaseSourceService.create(newSource("Salesforce"));

        List<KnowledgeBaseSource> sources =
            knowledgeBaseSourceService.getAllByIds(List.of(hubspot.getId(), salesforce.getId()));

        assertThat(sources).hasSize(2);
    }

    @Test
    void testGetAllEnabledByIdsSkipsDisabled() {
        KnowledgeBaseSource enabled = knowledgeBaseSourceService.create(newSource("Enabled HubSpot"));
        KnowledgeBaseSource disabled = knowledgeBaseSourceService.create(newSource("Disabled HubSpot"));

        knowledgeBaseSourceService.setEnabled(disabled.getId(), false);

        List<KnowledgeBaseSource> sources =
            knowledgeBaseSourceService.getAllEnabledByIds(List.of(enabled.getId(), disabled.getId()));

        assertThat(sources)
            .hasSize(1)
            .extracting(KnowledgeBaseSource::getId)
            .containsExactly(enabled.getId());
    }

    @Test
    void testUpdateStatusFlipsStatusAndTimestamps() {
        KnowledgeBaseSource created = knowledgeBaseSourceService.create(newSource("HubSpot"));

        Instant lastSyncRunAt = Instant.parse("2026-05-08T12:00:00Z");

        knowledgeBaseSourceService.updateStatus(
            created.getId(), KnowledgeBaseSourceStatus.READY, lastSyncRunAt, 42L);

        KnowledgeBaseSource refreshed = knowledgeBaseSourceService.get(created.getId());

        assertThat(refreshed.getStatus()).isEqualTo(KnowledgeBaseSourceStatus.READY);
        assertThat(refreshed.getLastSyncRunAt()).isEqualTo(lastSyncRunAt);
        assertThat(refreshed.getLastSyncJobExecutionId()).isEqualTo(42L);
    }

    @Test
    void testUpdateLastSyncMetadataDoesNotChangeStatus() {
        KnowledgeBaseSource created = knowledgeBaseSourceService.create(newSource("HubSpot"));

        // Establish a non-default status first.
        knowledgeBaseSourceService.updateStatus(
            created.getId(), KnowledgeBaseSourceStatus.PREVIEW, Instant.parse("2026-05-08T11:00:00Z"), 11L);

        Instant newSyncRun = Instant.parse("2026-05-08T13:00:00Z");

        knowledgeBaseSourceService.updateLastSyncMetadata(created.getId(), newSyncRun, 99L);

        KnowledgeBaseSource refreshed = knowledgeBaseSourceService.get(created.getId());

        assertThat(refreshed.getStatus()).isEqualTo(KnowledgeBaseSourceStatus.PREVIEW);
        assertThat(refreshed.getLastSyncRunAt()).isEqualTo(newSyncRun);
        assertThat(refreshed.getLastSyncJobExecutionId()).isEqualTo(99L);
    }

    @Test
    void testSetEnabledTogglesFlag() {
        KnowledgeBaseSource created = knowledgeBaseSourceService.create(newSource("HubSpot"));

        assertThat(created.isEnabled()).isTrue();

        knowledgeBaseSourceService.setEnabled(created.getId(), false);

        assertThat(knowledgeBaseSourceService.get(created.getId())
            .isEnabled()).isFalse();

        knowledgeBaseSourceService.setEnabled(created.getId(), true);

        assertThat(knowledgeBaseSourceService.get(created.getId())
            .isEnabled()).isTrue();
    }

    private KnowledgeBaseSource newSource(String name) {
        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setName(name);
        source.setSourceComponentName("hubspot");
        source.setSourceComponentVersion(1);
        source.setSourceClusterElementName("contactsReader");
        source.setKnowledgeBaseId(knowledgeBase.getId());
        source.setCadence("@hourly");
        source.setStatus(KnowledgeBaseSourceStatus.BUILDING_PREVIEW);

        return source;
    }
}
