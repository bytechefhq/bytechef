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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class InteractivePickerToolCallbacksTest {

    @Test
    void testWriteExposesTheTenPickersInOrder() {
        InteractivePickerToolCallbacks pickers = pickers(null);

        assertThat(names(pickers.write())).containsExactly(
            "listConnectionsForComponent", "selectConnection", "lookupActionPropertyOptions",
            "lookupTriggerPropertyOptions", "lookupClusterElementPropertyOptions", "selectPropertyOption",
            "selectTriggerPropertyOption", "selectClusterElementPropertyOption", "askUserQuestion",
            "createConnection");
    }

    @Test
    void testReadExposesOnlyNonMutatingPickers() {
        InteractivePickerToolCallbacks pickers = pickers(null);

        assertThat(names(pickers.read())).containsExactly(
            "listConnectionsForComponent", "lookupActionPropertyOptions", "lookupTriggerPropertyOptions",
            "lookupClusterElementPropertyOptions", "askUserQuestion");
    }

    @Test
    void testEmbeddedListerDoesNotChangeTheExposedPickers() {
        InteractivePickerToolCallbacks pickers = pickers(mock(CopilotConnectionLister.class));

        assertThat(names(pickers.write())).hasSize(10);
        assertThat(names(pickers.read())).hasSize(5);
    }

    @Test
    void testEachCallBuildsFreshInstances() {
        InteractivePickerToolCallbacks pickers = pickers(null);

        assertThat(pickers.write()
            .get(0)).isNotSameAs(pickers.write()
                .get(0));
    }

    private static List<String> names(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }

    @SuppressWarnings("unchecked")
    private static InteractivePickerToolCallbacks pickers(CopilotConnectionLister lister) {
        ObjectProvider<CopilotConnectionLister> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(lister);

        return new InteractivePickerToolCallbacks(
            mock(ConnectionDefinitionService.class), mock(WorkspaceConnectionFacade.class),
            mock(ComponentDefinitionService.class), mock(ActionDefinitionService.class),
            mock(ActionDefinitionFacade.class), mock(TriggerDefinitionService.class),
            mock(TriggerDefinitionFacade.class), mock(ClusterElementDefinitionService.class),
            mock(ClusterElementDefinitionFacade.class), mock(PropertyOptionsResolver.class), provider,
            mock(ScopedConnectionGuard.class));
    }
}
