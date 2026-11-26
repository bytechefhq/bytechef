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

import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnvironmentPromotionFacadeTest {

    private final EnvironmentService environmentService = () -> List.of(Environment.values());

    @Test
    void testDuplicateResourceTypeFailsConstruction() {
        List<EnvironmentPromotionContributor> contributors = List.of(
            new FirstDataTableContributor(), new SecondDataTableContributor());

        assertThatThrownBy(() -> newFacade(contributors))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(FirstDataTableContributor.class.getName())
            .hasMessageContaining(SecondDataTableContributor.class.getName());
    }

    @Test
    void testUnknownResourceTypeFails() {
        EnvironmentPromotionFacadeImpl environmentPromotionFacade = newFacade(List.of(new RecordingContributor()));

        assertThatThrownBy(() -> environmentPromotionFacade.preview("FOO", 7L, 0L, 1L))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage("No promotion contributor registered for resource type FOO");
    }

    @Test
    void testDispatchesWithBothEnvironments() {
        RecordingContributor recordingContributor = new RecordingContributor();

        EnvironmentPromotionFacadeImpl environmentPromotionFacade = newFacade(List.of(recordingContributor));

        environmentPromotionFacade.preview("X", 7L, 0L, 1L);

        assertThat(recordingContributor.sourceId).isEqualTo(7L);
        assertThat(recordingContributor.sourceEnvironment).isEqualTo(Environment.DEVELOPMENT);
        assertThat(recordingContributor.targetEnvironment).isEqualTo(Environment.STAGING);
    }

    @Test
    void testPromoteDispatchesWithBothEnvironments() {
        RecordingContributor recordingContributor = new RecordingContributor();

        EnvironmentPromotionFacadeImpl environmentPromotionFacade = newFacade(List.of(recordingContributor));

        environmentPromotionFacade.promote("X", 7L, 1L, 2L, Map.of(10L, 20L), null);

        assertThat(recordingContributor.sourceId).isEqualTo(7L);
        assertThat(recordingContributor.sourceEnvironment).isEqualTo(Environment.STAGING);
        assertThat(recordingContributor.targetEnvironment).isEqualTo(Environment.PRODUCTION);
        assertThat(recordingContributor.connectionMappings).isEqualTo(Map.of(10L, 20L));
    }

    @Test
    void testPromotePassesExpectedHashToContributor() {
        RecordingContributor recordingContributor = new RecordingContributor();

        EnvironmentPromotionFacadeImpl environmentPromotionFacade = newFacade(List.of(recordingContributor));

        environmentPromotionFacade.promote("X", 7L, 1L, 2L, Map.of(), "expected-hash");

        assertThat(recordingContributor.expectedDestructiveChangesHash).isEqualTo("expected-hash");
    }

    @Test
    void testSameSourceAndTargetEnvironmentFails() {
        RecordingContributor recordingContributor = new RecordingContributor();

        EnvironmentPromotionFacadeImpl environmentPromotionFacade = newFacade(List.of(recordingContributor));

        assertThatThrownBy(() -> environmentPromotionFacade.preview("X", 7L, 1L, 1L))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());

        assertThat(recordingContributor.sourceEnvironment).isNull();
    }

    @Test
    void testPromoteToTheSameEnvironmentFails() {
        RecordingContributor recordingContributor = new RecordingContributor();

        EnvironmentPromotionFacadeImpl environmentPromotionFacade = newFacade(List.of(recordingContributor));

        assertThatThrownBy(() -> environmentPromotionFacade.promote("X", 7L, 2L, 2L, Map.of(), null))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());

        assertThat(recordingContributor.sourceEnvironment).isNull();
    }

    @Test
    void testUnknownSourceEnvironmentFails() {
        RecordingContributor recordingContributor = new RecordingContributor();

        EnvironmentPromotionFacadeImpl environmentPromotionFacade = newFacade(List.of(recordingContributor));

        assertThatThrownBy(() -> environmentPromotionFacade.preview("X", 7L, 99L, 1L))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.ENVIRONMENT_NOT_AVAILABLE.getErrorKey());

        assertThat(recordingContributor.sourceEnvironment).isNull();
    }

    @Test
    void testUnknownTargetEnvironmentFails() {
        RecordingContributor recordingContributor = new RecordingContributor();

        EnvironmentPromotionFacadeImpl environmentPromotionFacade = newFacade(List.of(recordingContributor));

        assertThatThrownBy(() -> environmentPromotionFacade.promote("X", 7L, 0L, 99L, Map.of(), null))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.ENVIRONMENT_NOT_AVAILABLE.getErrorKey());

        assertThat(recordingContributor.sourceEnvironment).isNull();
    }

    @SuppressWarnings("unchecked")
    private EnvironmentPromotionFacadeImpl newFacade(List<EnvironmentPromotionContributor> contributors) {
        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        return new EnvironmentPromotionFacadeImpl(environmentService, contributors, meterRegistryProvider);
    }

    private static class RecordingContributor implements EnvironmentPromotionContributor {

        private Map<Long, Long> connectionMappings;
        private String expectedDestructiveChangesHash;
        private long sourceId;
        private Environment sourceEnvironment;
        private Environment targetEnvironment;

        @Override
        public String getResourceType() {
            return "X";
        }

        @Override
        public PlatformType getPlatformType() {
            return PlatformType.AUTOMATION;
        }

        @Override
        public EnvironmentPromotionPreview preview(
            long sourceId, Environment sourceEnvironment, Environment targetEnvironment) {

            this.sourceId = sourceId;
            this.sourceEnvironment = sourceEnvironment;
            this.targetEnvironment = targetEnvironment;

            return new EnvironmentPromotionPreview(
                "X", sourceId, sourceEnvironment, targetEnvironment, null, null, List.of(), List.of(), List.of(),
                List.of(), List.of(), "");
        }

        @Override
        public EnvironmentPromotionResult promote(
            long sourceId, Environment sourceEnvironment, Environment targetEnvironment,
            Map<Long, Long> connectionMappings, @Nullable String expectedDestructiveChangesHash) {

            this.connectionMappings = Map.copyOf(connectionMappings);
            this.expectedDestructiveChangesHash = expectedDestructiveChangesHash;
            this.sourceId = sourceId;
            this.sourceEnvironment = sourceEnvironment;
            this.targetEnvironment = targetEnvironment;

            return new EnvironmentPromotionResult(70L, true, null, List.of());
        }
    }

    private static class FirstDataTableContributor extends RecordingContributor {

        @Override
        public String getResourceType() {
            return "DATA_TABLE";
        }
    }

    private static class SecondDataTableContributor extends RecordingContributor {

        @Override
        public String getResourceType() {
            return "DATA_TABLE";
        }
    }
}
