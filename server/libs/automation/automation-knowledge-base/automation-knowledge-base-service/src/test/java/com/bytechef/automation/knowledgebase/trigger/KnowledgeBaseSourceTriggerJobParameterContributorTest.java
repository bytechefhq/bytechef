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

        Map<String, ?> result = contributor.contribute(Map.of("someOtherKey", "value"), null);

        assertThat(result).isEmpty();
    }

    @Test
    void testMissingSourceRowReturnsEmptyMap() {
        KnowledgeBaseSourceService service = mock(KnowledgeBaseSourceService.class);

        when(service.fetch(eq(42L))).thenReturn(Optional.empty());

        var contributor = new KnowledgeBaseSourceTriggerJobParameterContributor(service);

        Map<String, ?> result = contributor.contribute(Map.of("knowledgeBaseSourceId", 42L), null);

        assertThat(result).isEmpty();
    }

    @Test
    void testNullLastSyncRunAtReturnsEmptyMap() {
        KnowledgeBaseSource source = new KnowledgeBaseSource();

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

        Map<String, ?> result = contributor.contribute(Map.of("knowledgeBaseSourceId", "42"), null);

        assertThat(result).hasSize(1);
        assertThat(result.get("datastream.since")).isEqualTo(lastSyncRunAt.toEpochMilli());
    }
}
