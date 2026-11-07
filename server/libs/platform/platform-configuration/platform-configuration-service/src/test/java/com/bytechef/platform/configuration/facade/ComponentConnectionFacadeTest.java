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

package com.bytechef.platform.configuration.facade;

import static com.bytechef.platform.configuration.facade.TriggerClusterRootWorkflowFixture.TRIGGER_NAME;
import static com.bytechef.platform.configuration.facade.TriggerClusterRootWorkflowFixture.VOICE_AGENT_NAME;
import static com.bytechef.platform.configuration.facade.TriggerClusterRootWorkflowFixture.WORKFLOW_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactoryResolver;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ComponentConnectionFacadeTest {

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private ComponentConnectionFactoryResolver componentConnectionFactoryResolver;

    @Mock
    private WorkflowService workflowService;

    private ComponentConnectionFacadeImpl componentConnectionFacade;

    @BeforeEach
    void setUp() {
        componentConnectionFacade = new ComponentConnectionFacadeImpl(
            clusterElementDefinitionService, List.of(), componentDefinitionService,
            componentConnectionFactoryResolver, workflowService);
    }

    @Test
    void testGetClusterElementComponentConnectionsResolvesVoiceAgentUnderTrigger() {
        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(TriggerClusterRootWorkflowFixture.workflow());
        when(clusterElementDefinitionService.getClusterElementType("browser", 1, "VOICEAGENT"))
            .thenReturn(new ClusterElementType("VOICE_AGENT", "voiceAgent", "Voice Agent"));

        ComponentDefinition deepgramComponentDefinition = mock(ComponentDefinition.class);

        when(deepgramComponentDefinition.getConnection()).thenReturn(mock(ConnectionDefinition.class));
        when(deepgramComponentDefinition.getName()).thenReturn("deepgram");
        when(deepgramComponentDefinition.getVersion()).thenReturn(1);
        when(deepgramComponentDefinition.isConnectionRequired()).thenReturn(true);
        when(componentDefinitionService.fetchComponentDefinition("deepgram", 1))
            .thenReturn(Optional.of(deepgramComponentDefinition));

        List<ComponentConnection> componentConnections =
            componentConnectionFacade.getClusterElementComponentConnections(
                WORKFLOW_ID, TRIGGER_NAME, "voiceAgent", VOICE_AGENT_NAME);

        assertThat(componentConnections)
            .singleElement()
            .satisfies(componentConnection -> {
                assertThat(componentConnection.componentName()).isEqualTo("deepgram");
                assertThat(componentConnection.key()).isEqualTo(VOICE_AGENT_NAME);
                assertThat(componentConnection.required()).isTrue();
            });
    }
}
