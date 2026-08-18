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

import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the seven project-deployment CRUD tool names {@link AiHubConfiguration#deploymentFlatCrudToolCallbacks} restores
 * flat on the hub surfaces (ticket 732, Task 3 of the CRUD-delegate unwind), replacing the dissolved
 * {@code project_deployment_agent} delegate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationDeploymentFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES = Set.of("listProjectDeployments");

    private static final Set<String> EXPECTED_WRITE_NAMES = Set.of(
        "listProjectDeployments", "createProjectDeployment", "updateProjectDeployment", "deleteProjectDeployment",
        "rollbackProjectDeployment", "toggleProjectDeployment", "promoteWorkflow");

    @Test
    void testReadModeReturnsExactlyTheOneReadName() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.deploymentFlatCrudToolCallbacks(present(), false);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testWriteModeReturnsExactlyTheSevenNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.deploymentFlatCrudToolCallbacks(present(), true);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_WRITE_NAMES);
    }

    @Test
    void testAbsentFactoryReturnsEmptyListForBothModes() {
        assertThat(AiHubConfiguration.deploymentFlatCrudToolCallbacks(absent(), false)).isEmpty();
        assertThat(AiHubConfiguration.deploymentFlatCrudToolCallbacks(absent(), true)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DeploymentToolCallbacksFactory> present() {
        DeploymentToolCallbacksFactory factory = new DeploymentToolCallbacksFactory(
            mock(ProjectDeploymentFacade.class));

        ObjectProvider<DeploymentToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DeploymentToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
