/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.promotion.config.EnvironmentPromotionIntTestConfiguration;
import com.bytechef.ee.automation.promotion.config.TestResourcePromotionContributor;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EnvironmentPromotionIntTestConfiguration.class)
class EnvironmentPromotionFacadeIntTest {

    @Autowired
    private EnvironmentPromotionFacade environmentPromotionFacade;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void testPreviewDispatchesToTheRegisteredContributorBean() {
        EnvironmentPromotionPreview preview = environmentPromotionFacade.preview(
            TestResourcePromotionContributor.RESOURCE_TYPE, 9L, Environment.DEVELOPMENT.ordinal(),
            Environment.STAGING.ordinal());

        assertThat(preview.resourceType()).isEqualTo(TestResourcePromotionContributor.RESOURCE_TYPE);
        assertThat(preview.sourceId()).isEqualTo(9L);
        assertThat(preview.sourceEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(preview.targetEnvironment()).isEqualTo(Environment.STAGING);
    }

    @Test
    void testPromoteDispatchesToTheRegisteredContributorBeanAndRecordsTheOutcome() {
        double createdCountBefore = createdCount();

        EnvironmentPromotionResult result = environmentPromotionFacade.promote(
            TestResourcePromotionContributor.RESOURCE_TYPE, 9L, Environment.DEVELOPMENT.ordinal(),
            Environment.STAGING.ordinal(), Map.of(), null);

        assertThat(result.targetId()).isEqualTo(TestResourcePromotionContributor.TARGET_ID);
        assertThat(result.created()).isTrue();
        assertThat(createdCount()).isEqualTo(createdCountBefore + 1.0);
    }

    private double createdCount() {
        Counter counter = meterRegistry.counter(
            "bytechef_environment_promotion", "resource", "test_resource", "outcome", "created");

        return counter.count();
    }

    @Nested
    class Dispatch {
        private final EnvironmentService environmentService = mock(EnvironmentService.class);
        private final EnvironmentPromotionContributor contributor = mock(EnvironmentPromotionContributor.class);
        private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        private EnvironmentPromotionFacadeImpl facade;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void beforeEach() {
            when(contributor.getResourceType()).thenReturn("TEST_RESOURCE");
            when(environmentService.getEnvironments())
                .thenReturn(List.of(Environment.DEVELOPMENT, Environment.STAGING, Environment.PRODUCTION));
            when(environmentService.getEnvironment(Environment.DEVELOPMENT.ordinal()))
                .thenReturn(Environment.DEVELOPMENT);
            when(environmentService.getEnvironment(Environment.STAGING.ordinal())).thenReturn(Environment.STAGING);

            ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

            when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

            facade =
                new EnvironmentPromotionFacadeImpl(environmentService, List.of(contributor), meterRegistryProvider);
        }

        @Test
        void testPreviewDispatchesToContributorByResourceType() {
            EnvironmentPromotionPreview preview = new EnvironmentPromotionPreview(
                "TEST_RESOURCE", 9L, Environment.DEVELOPMENT, Environment.STAGING, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), "");

            when(contributor.preview(9L, Environment.DEVELOPMENT, Environment.STAGING)).thenReturn(preview);

            assertThat(
                facade.preview("TEST_RESOURCE", 9L, Environment.DEVELOPMENT.ordinal(), Environment.STAGING.ordinal()))
                    .isSameAs(preview);
        }

        @Test
        void testUnavailableTargetEnvironmentIsRejected() {
            when(environmentService.getEnvironments()).thenReturn(List.of(Environment.DEVELOPMENT));

            assertThatThrownBy(
                () -> facade.preview("TEST_RESOURCE", 9L, Environment.DEVELOPMENT.ordinal(),
                    Environment.STAGING.ordinal()))
                        .isInstanceOf(ConfigurationException.class);
        }

        @Test
        void testUnknownResourceTypeIsRejected() {
            assertThatThrownBy(
                () -> facade.preview("UNKNOWN_RESOURCE", 9L, Environment.DEVELOPMENT.ordinal(),
                    Environment.STAGING.ordinal()))
                        .isInstanceOf(ConfigurationException.class);
        }

        @Test
        void testPromoteRecordsCreatedOutcome() {
            when(contributor.promote(9L, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null))
                .thenReturn(new EnvironmentPromotionResult(77L, true, null, List.of()));

            facade.promote("TEST_RESOURCE", 9L, Environment.DEVELOPMENT.ordinal(), Environment.STAGING.ordinal(),
                Map.of(), null);

            Counter createdCounter = meterRegistry.counter(
                "bytechef_environment_promotion", "resource", "test_resource", "outcome", "created");

            assertThat(createdCounter.count()).isEqualTo(1.0);
        }

        @Test
        void testPromoteRecordsUpdatedOutcome() {
            when(contributor.promote(9L, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null))
                .thenReturn(new EnvironmentPromotionResult(77L, false, null, List.of()));

            facade.promote("TEST_RESOURCE", 9L, Environment.DEVELOPMENT.ordinal(), Environment.STAGING.ordinal(),
                Map.of(), null);

            Counter updatedCounter = meterRegistry.counter(
                "bytechef_environment_promotion", "resource", "test_resource", "outcome", "updated");

            assertThat(updatedCounter.count()).isEqualTo(1.0);
        }

        @Test
        void testPromoteRecordsFailedOutcomeAndRethrows() {
            when(contributor.promote(9L, Environment.DEVELOPMENT, Environment.STAGING, Map.of(), null))
                .thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(
                () -> facade.promote("TEST_RESOURCE", 9L, Environment.DEVELOPMENT.ordinal(),
                    Environment.STAGING.ordinal(),
                    Map.of(), null))
                        .isInstanceOf(IllegalStateException.class);
            Counter failedCounter = meterRegistry.counter(
                "bytechef_environment_promotion", "resource", "test_resource", "outcome", "failed");

            assertThat(failedCounter.count()).isEqualTo(1.0);
        }

        @Test
        @SuppressWarnings("unchecked")
        void testDuplicateResourceTypeContributorsFailFast() {
            EnvironmentPromotionContributor duplicateContributor = mock(EnvironmentPromotionContributor.class);

            when(duplicateContributor.getResourceType()).thenReturn("TEST_RESOURCE");

            ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

            assertThatThrownBy(() -> new EnvironmentPromotionFacadeImpl(
                environmentService, List.of(contributor, duplicateContributor), meterRegistryProvider))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void testNoRegisteredContributorsRejectsEveryResourceType() {
            ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

            when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

            EnvironmentPromotionFacadeImpl emptyFacade =
                new EnvironmentPromotionFacadeImpl(environmentService, List.of(), meterRegistryProvider);

            assertThatThrownBy(
                () -> emptyFacade.preview("TEST_RESOURCE", 9L, Environment.DEVELOPMENT.ordinal(),
                    Environment.STAGING.ordinal()))
                        .isInstanceOf(ConfigurationException.class);
        }
    }
}
