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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class TriggerDefinitionFacadeTest {

    @Mock
    private ConnectionService connectionService;

    @Mock
    private TriggerDefinitionService triggerDefinitionService;

    private TriggerDefinitionFacadeImpl triggerDefinitionFacade;

    @BeforeEach
    void beforeEach() {
        triggerDefinitionFacade = new TriggerDefinitionFacadeImpl(connectionService, triggerDefinitionService);
    }

    @Test
    void testExecuteTriggerWithPendingReassignmentConnectionThrows() {
        when(connectionService.getConnection(1L)).thenReturn(createConnection(ConnectionStatus.PENDING_REASSIGNMENT));

        assertThatThrownBy(
            () -> triggerDefinitionFacade.executeTrigger(
                "testComponent", 1, "testTrigger", null, null, null, Map.of(), null, null, 1L, null,
                PlatformType.AUTOMATION, true))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Test Connection")
                    .hasMessageContaining("PENDING_REASSIGNMENT");

        verifyNoInteractions(triggerDefinitionService);
    }

    @Test
    void testExecuteTriggerWithRevokedConnectionThrows() {
        when(connectionService.getConnection(1L)).thenReturn(createConnection(ConnectionStatus.REVOKED));

        assertThatThrownBy(
            () -> triggerDefinitionFacade.executeTrigger(
                "testComponent", 1, "testTrigger", null, null, null, Map.of(), null, null, 1L, null,
                PlatformType.AUTOMATION, true))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("REVOKED");

        verifyNoInteractions(triggerDefinitionService);
    }

    @Test
    void testExecuteWebhookDisableWithPendingReassignmentConnectionStillTearsDown() {
        when(connectionService.getConnection(1L)).thenReturn(createConnection(ConnectionStatus.PENDING_REASSIGNMENT));

        assertThatCode(
            () -> triggerDefinitionFacade.executeWebhookDisable(
                "testComponent", 1, "testTrigger", Map.of(), "workflowExecutionId", Map.of(), 1L))
                    .doesNotThrowAnyException();

        verify(triggerDefinitionService).executeWebhookDisable(
            eq("testComponent"), eq(1), eq("testTrigger"), any(), any(), any(), any(ComponentConnection.class));
    }

    private static Connection createConnection(ConnectionStatus connectionStatus) {
        return Connection.builder()
            .componentName("testComponent")
            .connectionVersion(1)
            .name("Test Connection")
            .parameters(Map.of())
            .status(connectionStatus)
            .type(PlatformType.AUTOMATION)
            .build();
    }

    @Disabled
    @Test
    public void testPerformDynamicWebhookDisable() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformDynamicWebhookEnable() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformDynamicWebhookRefresh() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformListenerDisable() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformListenerEnable() {
        // TODO
    }
}
