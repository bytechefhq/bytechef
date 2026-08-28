/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.constant.PlatformType;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationComponentDefinitionFilterTest {

    private final IntegrationComponentDefinitionFilter filter = new IntegrationComponentDefinitionFilter();

    /**
     * Per-account data work belongs in bridged automation projects, where the owner filter governs it. An integration
     * workflow is the vendor's connector surface, so the two stores are not offered there.
     */
    @Test
    void testDataTableAndKnowledgeBaseAreNotOfferedInIntegrationWorkflows() {
        assertFalse(filter.filter(componentDefinition("dataTable")));
        assertFalse(filter.filter(componentDefinition("knowledgeBase")));
    }

    @Test
    void testAnOrdinaryConnectorIsStillOffered() {
        assertTrue(filter.filter(componentDefinition("slack")));
    }

    /**
     * A second {@code ComponentDefinitionFilter} bean for EMBEDDED would not work --
     * {@code ComponentDefinitionServiceImpl} selects with {@code findFirst()} -- so this filter has to stay the only
     * one, and has to keep supporting only its own platform type.
     */
    @Test
    void testTheFilterSupportsEmbeddedOnly() {
        assertTrue(filter.supports(PlatformType.EMBEDDED));
        assertFalse(filter.supports(PlatformType.AUTOMATION));
    }

    private static ComponentDefinition componentDefinition(String name) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn(name);

        return componentDefinition;
    }
}
