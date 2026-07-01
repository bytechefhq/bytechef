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

package com.bytechef.platform.component.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ComponentDefinitionFacadeImplTest {

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private ConnectionService connectionService;

    private ComponentDefinitionFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new ComponentDefinitionFacadeImpl(componentDefinitionService, connectionService);
    }

    @Test
    void testExecuteWorkflowInputOptionsResolvesActiveConnectionAndDelegates() {
        Connection connection = Connection.builder()
            .componentName("slack")
            .connectionVersion(1)
            .name("Slack Connection")
            .parameters(Map.of())
            .type(PlatformType.AUTOMATION)
            .build();

        connection.setCredentialStatus(CredentialStatus.VALID);

        when(connectionService.getConnection(5L)).thenReturn(connection);
        when(componentDefinitionService.executeWorkflowInputOptions(
            eq("slack"), eq(1), eq("channel"), eq("channel"), anyMap(), anyList(), any(), any()))
                .thenReturn(List.of(new Option(ComponentDsl.option("General", "C1"))));

        List<Option> options = facade.executeWorkflowInputOptions(
            "slack", 1, "channel", "channel", Map.of(), List.of(), null, 5L);

        assertThat(options)
            .extracting(Option::getValue)
            .containsExactly("C1");
    }

    @Test
    void testExecuteWorkflowInputOptionsNullConnectionPassesNull() {
        when(componentDefinitionService.executeWorkflowInputOptions(
            anyString(), anyInt(), anyString(), anyString(), anyMap(), anyList(), any(), isNull()))
                .thenReturn(List.of());

        List<Option> options = facade.executeWorkflowInputOptions(
            "slack", 1, "channel", "channel", Map.of(), List.of(), null, null);

        verify(connectionService, never()).getConnection(any(Long.class));

        assertThat(options).isEmpty();
    }

    @Test
    void testExecuteWorkflowInputOptionsInactiveConnectionThrows() {
        Connection connection = Connection.builder()
            .componentName("slack")
            .connectionVersion(1)
            .name("Slack Connection")
            .parameters(Map.of())
            .type(PlatformType.AUTOMATION)
            .build();

        connection.setCredentialStatus(CredentialStatus.INVALID);

        when(connectionService.getConnection(5L)).thenReturn(connection);

        assertThatThrownBy(() -> facade.executeWorkflowInputOptions(
            "slack", 1, "channel", "channel", Map.of(), List.of(), null, 5L))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Slack Connection")
                .hasMessageContaining("INVALID");
    }
}
