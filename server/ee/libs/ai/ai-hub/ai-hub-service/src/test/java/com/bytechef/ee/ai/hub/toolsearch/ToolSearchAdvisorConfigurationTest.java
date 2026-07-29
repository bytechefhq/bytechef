/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchAdvisorConfiguration.AiHubClusterElementToolCallbacks;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolSearchAdvisorConfigurationTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final ToolSearchAdvisorConfiguration configuration = new ToolSearchAdvisorConfiguration();

    @Test
    void testClusterElementToolCallbacksBeanDefersCatalogLoadUntilFirstUse() {
        when(clusterElementDefinitionService.getClusterElementDefinitionStubs(BaseToolFunction.TOOLS))
            .thenReturn(List.of());

        AiHubClusterElementToolCallbacks callbacks = configuration.aiHubClusterElementToolCallbacks(
            clusterElementDefinitionService, connectionService);

        // Building the bean must not enumerate cluster elements: that call forces the stub catalog to load, which is
        // exactly the startup cost this bean is meant to avoid (the advisor beans inject it eagerly).
        verify(clusterElementDefinitionService, never()).getClusterElementDefinitionStubs(any());

        Map<?, ?> resolved = callbacks.callbacks()
            .get();

        // First use resolves the catalog once...
        assertThat(resolved).isEmpty();

        verify(clusterElementDefinitionService, times(1)).getClusterElementDefinitionStubs(BaseToolFunction.TOOLS);

        // ...and the memoised supplier does not re-query on subsequent access.
        callbacks.callbacks()
            .get();

        verify(clusterElementDefinitionService, times(1)).getClusterElementDefinitionStubs(BaseToolFunction.TOOLS);
    }
}
