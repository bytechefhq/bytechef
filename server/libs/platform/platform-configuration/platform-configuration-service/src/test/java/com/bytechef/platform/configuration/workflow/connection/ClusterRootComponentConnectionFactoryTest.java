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

package com.bytechef.platform.configuration.workflow.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ClusterRootComponentConnectionFactoryTest {

    @Test
    void testVoiceAgentElementOnATriggerYieldsItsConnection() {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
        ComponentDefinition browser = mock(ComponentDefinition.class);
        ComponentDefinition deepgram = mock(ComponentDefinition.class);

        when(browser.getConnection()).thenReturn(null);
        when(browser.isClusterRoot()).thenReturn(true);
        when(deepgram.getName()).thenReturn("deepgram");
        when(deepgram.getVersion()).thenReturn(1);
        when(deepgram.getConnection()).thenReturn(mock(ConnectionDefinition.class));
        when(deepgram.isConnectionRequired()).thenReturn(true);
        when(componentDefinitionService.getComponentDefinition("deepgram", 1)).thenReturn(deepgram);

        ClusterRootComponentConnectionFactory factory = new ClusterRootComponentConnectionFactory(
            List.of(), componentDefinitionService);

        List<ComponentConnection> connections = factory.create(
            "trigger_1",
            Map.of(
                "clusterElements", Map.of(
                    "voiceAgent", Map.of(
                        "name", "voiceAgent_1", "type", "deepgram/v1/voiceAgent", "parameters", Map.of()))),
            browser);

        assertThat(connections)
            .singleElement()
            .satisfies(connection -> {
                assertThat(connection.workflowNodeName()).isEqualTo("trigger_1");
                assertThat(connection.key()).isEqualTo("voiceAgent_1");
                assertThat(connection.componentName()).isEqualTo("deepgram");
                assertThat(connection.required()).isTrue();
            });
    }
}
