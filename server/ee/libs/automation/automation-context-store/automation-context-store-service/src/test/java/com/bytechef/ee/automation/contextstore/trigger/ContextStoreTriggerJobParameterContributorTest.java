/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContextStoreTriggerJobParameterContributor}. Mirrors the KB-side test — same six branches, same
 * fall-through-to-empty contract. See the KB test's Javadoc for the reasoning.
 *
 * @author Ivica Cardic
 * @version ee
 */
class ContextStoreTriggerJobParameterContributorTest {

    @Test
    void testHappyPathEmitsDatastreamSinceFromLastSyncRunAt() {
        Instant lastSyncRunAt = Instant.parse("2026-05-11T10:30:00Z");
        ContextStoreSource source = new ContextStoreSource();

        source.setLastSyncRunAt(lastSyncRunAt);

        ContextStoreSourceService service = mock(ContextStoreSourceService.class);

        when(service.fetch(eq(42L))).thenReturn(Optional.of(source));

        var contributor = new ContextStoreTriggerJobParameterContributor(service);

        Map<String, ?> result = contributor.contribute(Map.of("contextStoreSourceId", 42L), null);

        assertThat(result).hasSize(1);
        assertThat(result.get("datastream.since")).isEqualTo(lastSyncRunAt.toEpochMilli());
    }

    @Test
    void testMissingMetadataKeyReturnsEmptyMap() {
        ContextStoreSourceService service = mock(ContextStoreSourceService.class);

        var contributor = new ContextStoreTriggerJobParameterContributor(service);

        Map<String, ?> result = contributor.contribute(Map.of("someOtherKey", "value"), null);

        assertThat(result).isEmpty();
    }

    @Test
    void testMissingSourceRowReturnsEmptyMap() {
        ContextStoreSourceService service = mock(ContextStoreSourceService.class);

        when(service.fetch(eq(42L))).thenReturn(Optional.empty());

        var contributor = new ContextStoreTriggerJobParameterContributor(service);

        Map<String, ?> result = contributor.contribute(Map.of("contextStoreSourceId", 42L), null);

        assertThat(result).isEmpty();
    }

    @Test
    void testNullLastSyncRunAtReturnsEmptyMap() {
        ContextStoreSource source = new ContextStoreSource();
        // lastSyncRunAt deliberately unset — first sync hasn't completed yet.

        ContextStoreSourceService service = mock(ContextStoreSourceService.class);

        when(service.fetch(eq(42L))).thenReturn(Optional.of(source));

        var contributor = new ContextStoreTriggerJobParameterContributor(service);

        Map<String, ?> result = contributor.contribute(Map.of("contextStoreSourceId", 42L), null);

        assertThat(result).isEmpty();
    }

    @Test
    void testServiceExceptionReturnsEmptyMap() {
        ContextStoreSourceService service = mock(ContextStoreSourceService.class);

        when(service.fetch(eq(42L))).thenThrow(new RuntimeException("DB transient"));

        var contributor = new ContextStoreTriggerJobParameterContributor(service);

        Map<String, ?> result = contributor.contribute(Map.of("contextStoreSourceId", 42L), null);

        assertThat(result).isEmpty();
    }

    @Test
    void testStringEncodedSourceIdCoercesToLong() {
        Instant lastSyncRunAt = Instant.parse("2026-05-11T10:30:00Z");
        ContextStoreSource source = new ContextStoreSource();

        source.setLastSyncRunAt(lastSyncRunAt);

        ContextStoreSourceService service = mock(ContextStoreSourceService.class);

        when(service.fetch(eq(42L))).thenReturn(Optional.of(source));

        var contributor = new ContextStoreTriggerJobParameterContributor(service);

        Map<String, ?> result = contributor.contribute(Map.of("contextStoreSourceId", "42"), null);

        assertThat(result).hasSize(1);
        assertThat(result.get("datastream.since")).isEqualTo(lastSyncRunAt.toEpochMilli());
    }
}
