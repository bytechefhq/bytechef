/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.constant.PlatformType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationComponentDefinitionFilterTest {

    private final IntegrationComponentDefinitionFilter integrationComponentDefinitionFilter =
        new IntegrationComponentDefinitionFilter();

    @ParameterizedTest
    @ValueSource(strings = {
        "apiPlatform", "codeWorkflow", "dataTable", "knowledgeBase", "webhook"
    })
    void testFilterExcludesAutomationOnlyComponents(String componentName) {
        assertThat(integrationComponentDefinitionFilter.filter(new ComponentDefinition(componentName))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "aiAgent", "httpClient", "slack"
    })
    void testFilterKeepsOtherComponents(String componentName) {
        assertThat(integrationComponentDefinitionFilter.filter(new ComponentDefinition(componentName))).isTrue();
    }

    @Test
    void testSupportsOnlyEmbedded() {
        assertThat(integrationComponentDefinitionFilter.supports(PlatformType.EMBEDDED)).isTrue();
        assertThat(integrationComponentDefinitionFilter.supports(PlatformType.AUTOMATION)).isFalse();
    }
}
