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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.script.CodeEditorScriptInputProvider;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class CodeEditorScriptInputFacadeTest {

    private static final Map<String, Object> SOURCE_INPUT_PARAMETERS = Map.of("sheet", "Sheet1");

    @Mock
    private ConnectionService connectionService;

    @Mock
    private CodeEditorScriptInputProvider dataStreamProvider;

    @Mock
    private CodeEditorScriptInputProvider otherProvider;

    private CodeEditorScriptInputFacadeImpl codeEditorScriptInputFacade;

    @BeforeEach
    void setUp() {
        codeEditorScriptInputFacade = new CodeEditorScriptInputFacadeImpl(
            List.of(otherProvider, dataStreamProvider), connectionService);
    }

    @Test
    void testGetScriptInputParametersPicksProviderByRootNameAndBuildsConnectionFromId() {
        Connection connection = mock(Connection.class);

        when(connection.getComponentName()).thenReturn("googleSheets");
        when(connection.getConnectionVersion()).thenReturn(1);
        when(connection.getParameters()).thenAnswer(invocation -> Map.of("token", "secret"));
        when(connectionService.getConnection(21L)).thenReturn(connection);
        when(otherProvider.getRootComponentName()).thenReturn("other");
        when(dataStreamProvider.getRootComponentName()).thenReturn("dataStream");
        when(dataStreamProvider.getScriptInputParameters(
            eq(1), eq("googleSheets"), eq(2), eq("read"), eq(SOURCE_INPUT_PARAMETERS),
            any(ComponentConnection.class)))
                .thenReturn(Map.<String, Object>of("name", "Ada"));

        Map<String, Object> scriptInputParameters = codeEditorScriptInputFacade.getScriptInputParameters(
            "dataStream", 1, "googleSheets", 2, "read", SOURCE_INPUT_PARAMETERS, 21L);

        assertThat(scriptInputParameters).isEqualTo(Map.of("name", "Ada"));

        ArgumentCaptor<ComponentConnection> componentConnectionCaptor =
            ArgumentCaptor.forClass(ComponentConnection.class);

        verify(dataStreamProvider).getScriptInputParameters(
            eq(1), eq("googleSheets"), eq(2), eq("read"), eq(SOURCE_INPUT_PARAMETERS),
            componentConnectionCaptor.capture());

        ComponentConnection componentConnection = componentConnectionCaptor.getValue();

        assertThat(componentConnection.getComponentName()).isEqualTo("googleSheets");
        assertThat(componentConnection.getVersion()).isEqualTo(1);
        assertThat(componentConnection.getConnectionId()).isEqualTo(21L);
        assertThat(componentConnection.getParameters()).isEqualTo(Map.of("token", "secret"));
        verify(otherProvider, never()).getScriptInputParameters(
            anyInt(), anyString(), anyInt(), anyString(), anyMap(), any());
    }

    @Test
    void testGetScriptInputParametersWithoutConnectionIdPassesNoConnection() {
        when(otherProvider.getRootComponentName()).thenReturn("other");
        when(dataStreamProvider.getRootComponentName()).thenReturn("dataStream");
        when(dataStreamProvider.getScriptInputParameters(
            eq(1), eq("csvFile"), eq(1), eq("read"), eq(SOURCE_INPUT_PARAMETERS), isNull()))
                .thenReturn(Map.<String, Object>of("id", 1));

        assertThat(
            codeEditorScriptInputFacade.getScriptInputParameters(
                "dataStream", 1, "csvFile", 1, "read", SOURCE_INPUT_PARAMETERS, null))
                    .isEqualTo(Map.of("id", 1));
        verifyNoInteractions(connectionService);
    }

    @Test
    void testGetScriptInputParametersReturnsEmptyMapWhenNoProviderHandlesTheRootComponent() {
        when(otherProvider.getRootComponentName()).thenReturn("other");
        when(dataStreamProvider.getRootComponentName()).thenReturn("dataStream");

        assertThat(
            codeEditorScriptInputFacade.getScriptInputParameters(
                "aiAgent", 1, "googleSheets", 2, "read", SOURCE_INPUT_PARAMETERS, 21L))
                    .isEmpty();
        verifyNoInteractions(connectionService);
    }
}
