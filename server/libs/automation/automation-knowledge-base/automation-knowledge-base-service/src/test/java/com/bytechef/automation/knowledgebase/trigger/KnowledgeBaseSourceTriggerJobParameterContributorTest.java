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

package com.bytechef.automation.knowledgebase.trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link KnowledgeBaseSourceTriggerJobParameterContributor}. Exercises the four fall-through branches
 * that map to "first run = full pull" — missing metadata key, missing source row, missing {@code lastSyncRunAt}, and
 * lookup failure — plus the happy path where {@code datastream.since} is emitted.
 *
 * <p>
 * The {@code workflowExecutionId} argument is unused by the KB contributor; tests pass {@code null} for it to make that
 * explicit. The platform contract permits null here because the contributor keys solely off the metadata map.
 * </p>
 *
 * @author Ivica Cardic
 */
class KnowledgeBaseSourceTriggerJobParameterContributorTest {

    @Test
    void testHappyPathEmitsDatastreamSinceFromLastSyncRunAt() {
        Instant lastSyncRunAt = Instant.parse("2026-05-11T10:30:00Z");
        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setLastSyncRunAt(lastSyncRunAt);

        KnowledgeBaseSourceService service = mock(KnowledgeBaseSourceService.class);

        when(service.fetch(eq(42L))).thenReturn(Optional.of(source));

        var contributor = new KnowledgeBaseSourceTriggerJobParameterContributor(service);

        Map<String, ?> result = contributor.contribute(Map.of("knowledgeBaseSourceId", 42L), null);

        assertThat(result).hasSize(1);
        assertThat(result.get("datastream.since")).isEqualTo(lastSyncRunAt.toEpochMilli());
    }

    @Test
    void testMissingMetadataKeyReturnsEmptyMap() {
        KnowledgeBaseSourceService service = mock(KnowledgeBaseSourceService.class);

        var contributor = new KnowledgeBaseSourceTriggerJobParameterContributor(service);

        // A KB-source workflow always carries knowledgeBaseSourceId — other workflows must not be matched. The
        // contributor's silent no-op is what makes this safe to register globally without coordinator awareness.
        Map<String, ?> result = contributor.contribute(Map.of("someOtherKey", "value"), null);

        assertThat(result).isEmpty();
    }

    @Test
    void testMissingSourceRowReturnsEmptyMap() {
        KnowledgeBaseSourceService service = mock(KnowledgeBaseSourceService.class);

        when(service.fetch(eq(42L))).thenReturn(Optional.empty());

        var contributor = new KnowledgeBaseSourceTriggerJobParameterContributor(service);

        // Source deleted between trigger fire and job creation — the reader interprets a missing SINCE_KEY as
        // first run = full pull, which is the right recovery behavior. Don't fail the dispatch.
        Map<String, ?> result = contributor.contribute(Map.of("knowledgeBaseSourceId", 42L), null);

        assertThat(result).isEmpty();
    }

    @Test
    void testNullLastSyncRunAtReturnsEmptyMap() {
        KnowledgeBaseSource source = new KnowledgeBaseSource();
        // lastSyncRunAt deliberately not set — represents a freshly-created source whose first job has not yet
        // landed a completed run.

        KnowledgeBaseSourceService service = mock(KnowledgeBaseSourceService.class);

        when(service.fetch(eq(42L))).thenReturn(Optional.of(source));

        var contributor = new KnowledgeBaseSourceTriggerJobParameterContributor(service);

        Map<String, ?> result = contributor.contribute(Map.of("knowledgeBaseSourceId", 42L), null);

        assertThat(result).isEmpty();
    }

    @Test
    void testServiceExceptionReturnsEmptyMap() {
        KnowledgeBaseSourceService service = mock(KnowledgeBaseSourceService.class);

        when(service.fetch(eq(42L))).thenThrow(new RuntimeException("DB transient"));

        var contributor = new KnowledgeBaseSourceTriggerJobParameterContributor(service);

        // Lookup failures collapse to empty, mirroring the missing-row branch. The contract forbids throwing
        // because a transient DB hiccup would otherwise pin the trigger dispatch into the error path.
        Map<String, ?> result = contributor.contribute(Map.of("knowledgeBaseSourceId", 42L), null);

        assertThat(result).isEmpty();
    }

    @Test
    void testStringEncodedSourceIdCoercesToLong() {
        Instant lastSyncRunAt = Instant.parse("2026-05-11T10:30:00Z");
        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setLastSyncRunAt(lastSyncRunAt);

        KnowledgeBaseSourceService service = mock(KnowledgeBaseSourceService.class);

        when(service.fetch(eq(42L))).thenReturn(Optional.of(source));

        var contributor = new KnowledgeBaseSourceTriggerJobParameterContributor(service);

        // JSONB deserialization sometimes hands a numeric metadata value back as a String — particularly on
        // the cross-tenant remote-rest path. The contributor's toLong coercer accepts both.
        Map<String, ?> result = contributor.contribute(Map.of("knowledgeBaseSourceId", "42"), null);

        assertThat(result).hasSize(1);
        assertThat(result.get("datastream.since")).isEqualTo(lastSyncRunAt.toEpochMilli());
    }
}
