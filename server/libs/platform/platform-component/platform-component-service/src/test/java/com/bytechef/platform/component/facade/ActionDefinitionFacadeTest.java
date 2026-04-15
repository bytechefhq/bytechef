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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.Collections;
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
class ActionDefinitionFacadeTest {

    @Mock
    private ActionDefinitionService actionDefinitionService;

    @Mock
    private ConnectionService connectionService;

    private ActionDefinitionFacadeImpl actionDefinitionFacade;

    @BeforeEach
    void setUp() {
        actionDefinitionFacade = new ActionDefinitionFacadeImpl(connectionService, actionDefinitionService);
    }

    @Test
    void testExecuteOptionsWithActiveConnectionSucceeds() {
        Connection connection = Connection.builder()
            .componentName("testComponent")
            .connectionVersion(1)
            .name("Test Connection")
            .parameters(Map.of())
            .status(ConnectionStatus.ACTIVE)
            .type(PlatformType.AUTOMATION)
            .build();

        when(connectionService.getConnection(1L)).thenReturn(connection);
        when(
            actionDefinitionService.executeOptions(
                anyString(), anyInt(), anyString(), anyString(), anyMap(), anyList(), anyString(), any()))
                    .thenReturn(Collections.emptyList());

        assertThatCode(
            () -> actionDefinitionFacade.executeOptions(
                "testComponent", 1, "testAction", "testProperty", Map.of(), List.of(), "search", 1L))
                    .doesNotThrowAnyException();

        verify(connectionService).getConnection(1L);
    }

    @Test
    void testExecuteOptionsWithPendingReassignmentConnectionThrows() {
        Connection connection = Connection.builder()
            .componentName("testComponent")
            .connectionVersion(1)
            .name("Test Connection")
            .parameters(Map.of())
            .status(ConnectionStatus.PENDING_REASSIGNMENT)
            .type(PlatformType.AUTOMATION)
            .build();

        when(connectionService.getConnection(1L)).thenReturn(connection);

        assertThatThrownBy(
            () -> actionDefinitionFacade.executeOptions(
                "testComponent", 1, "testAction", "testProperty", Map.of(), List.of(), "search", 1L))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Test Connection")
                    .hasMessageContaining("PENDING_REASSIGNMENT");
    }

    @Test
    void testExecuteOptionsWithRevokedConnectionThrows() {
        Connection connection = Connection.builder()
            .componentName("testComponent")
            .connectionVersion(1)
            .name("Test Connection")
            .parameters(Map.of())
            .status(ConnectionStatus.REVOKED)
            .type(PlatformType.AUTOMATION)
            .build();

        when(connectionService.getConnection(1L)).thenReturn(connection);

        assertThatThrownBy(
            () -> actionDefinitionFacade.executeOptions(
                "testComponent", 1, "testAction", "testProperty", Map.of(), List.of(), "search", 1L))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Test Connection")
                    .hasMessageContaining("REVOKED");
    }

    @Test
    void testExecuteOptionsWithNullConnectionIdSucceeds() {
        when(
            actionDefinitionService.executeOptions(
                anyString(), anyInt(), anyString(), anyString(), anyMap(), anyList(), anyString(), isNull()))
                    .thenReturn(Collections.emptyList());

        List<Option> options = actionDefinitionFacade.executeOptions(
            "testComponent", 1, "testAction", "testProperty", Map.of(), List.of(), "search", null);

        verify(connectionService, never()).getConnection(any(Long.class));

        assertThat(options).isEmpty();
    }
}
