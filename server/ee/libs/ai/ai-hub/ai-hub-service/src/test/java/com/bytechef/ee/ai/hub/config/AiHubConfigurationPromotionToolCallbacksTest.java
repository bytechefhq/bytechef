/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalDefaults;
import com.bytechef.ee.automation.promotion.facade.EnvironmentPromotionFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 * @author Ivica Cardic
 */
class AiHubConfigurationPromotionToolCallbacksTest {

    @Test
    void testRegistersOnePromoteToolPerResourceType() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.promoteToEnvironmentToolCallbacks(
            present(mock(EnvironmentPromotionFacade.class)), present(mock(EnvironmentService.class)));

        assertThat(toolNames(toolCallbacks)).containsExactlyInAnyOrder(
            "promoteApiCollection", "promoteMcpServer", "promoteA2aServer", "promoteProjectDeployment");
    }

    @Test
    void testEveryPromoteToolIsGatedByDefault() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.promoteToEnvironmentToolCallbacks(
            present(mock(EnvironmentPromotionFacade.class)), present(mock(EnvironmentService.class)));

        assertThat(toolNames(toolCallbacks)).allMatch(AiHubToolApprovalDefaults::isGatedByDefault);
    }

    @Test
    void testRegistersNothingWithoutThePromotionFacade() {
        assertThat(
            AiHubConfiguration.promoteToEnvironmentToolCallbacks(absent(), present(mock(EnvironmentService.class))))
                .isEmpty();
    }

    @Test
    void testRegistersNothingWithoutTheEnvironmentService() {
        assertThat(
            AiHubConfiguration.promoteToEnvironmentToolCallbacks(
                present(mock(EnvironmentPromotionFacade.class)), absent()))
                    .isEmpty();
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> present(T bean) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(bean);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> absent() {
        return mock(ObjectProvider.class);
    }
}
