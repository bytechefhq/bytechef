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

package com.bytechef.platform.connection.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ConnectionFacadeTest {

    @Mock
    private ConnectionDefinitionService connectionDefinitionService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Mock
    private OAuth2Service oAuth2Service;

    @Mock
    private TagService tagService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressWarnings("unchecked")
    private final ObjectProvider<MeterRegistry> meterRegistryProvider =
        (ObjectProvider<MeterRegistry>) org.mockito.Mockito.mock(ObjectProvider.class);

    @Test
    void testEmbeddedCreateForcesPrivateVisibility() {
        ConnectionFacadeImpl facade = newFacade("EE");

        Connection persisted = new Connection();

        persisted.setId(1L);

        when(connectionService.create(any(Connection.class))).thenReturn(persisted);

        ConnectionDTO dto = ConnectionDTO.builder()
            .componentName("dummy")
            .name("c1")
            .visibility(ResourceVisibility.WORKSPACE)
            .build();

        facade.create(dto, PlatformType.EMBEDDED);

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        verify(connectionService).create(captor.capture());

        assertThat(captor.getValue()
            .getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
    }

    @Test
    void testCeAutomationCreateForcesWorkspaceVisibility() {
        ConnectionFacadeImpl facade = newFacade("CE");

        Connection persisted = new Connection();

        persisted.setId(2L);

        when(connectionService.create(any(Connection.class))).thenReturn(persisted);

        ConnectionDTO dto = ConnectionDTO.builder()
            .componentName("dummy")
            .name("c2")
            .visibility(ResourceVisibility.PRIVATE)
            .build();

        facade.create(dto, PlatformType.AUTOMATION);

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        verify(connectionService).create(captor.capture());

        assertThat(captor.getValue()
            .getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    @Test
    void testEeAutomationCreatePreservesPrivateVisibility() {
        ConnectionFacadeImpl facade = newFacade("EE");

        Connection persisted = new Connection();

        persisted.setId(3L);

        when(connectionService.create(any(Connection.class))).thenReturn(persisted);

        ConnectionDTO dto = ConnectionDTO.builder()
            .componentName("dummy")
            .name("c3")
            .visibility(ResourceVisibility.PRIVATE)
            .build();

        facade.create(dto, PlatformType.AUTOMATION);

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        verify(connectionService).create(captor.capture());

        assertThat(captor.getValue()
            .getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
    }

    @Test
    void testCeAutomationCreateForcesWorkspaceVisibilityRegardlessOfOrganizationRequest() {
        // Defense-in-depth: the UI hides the visibility picker in CE, but a hand-crafted request body must still
        // land as WORKSPACE whatever it asks for. ORGANIZATION is the interesting case because it is the one value
        // *above* the forced one — the override is an unconditional assignment, not a clamp, so widening requests
        // are coerced down just as narrowing ones are coerced up.
        ConnectionFacadeImpl facade = newFacade("CE");

        Connection persisted = new Connection();

        persisted.setId(4L);

        when(connectionService.create(any(Connection.class))).thenReturn(persisted);

        ConnectionDTO dto = ConnectionDTO.builder()
            .componentName("dummy")
            .name("c4")
            .visibility(ResourceVisibility.ORGANIZATION)
            .build();

        facade.create(dto, PlatformType.AUTOMATION);

        ArgumentCaptor<Connection> captor = ArgumentCaptor.forClass(Connection.class);

        verify(connectionService).create(captor.capture());

        assertThat(captor.getValue()
            .getVisibility()).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    private ConnectionFacadeImpl newFacade(String edition) {
        return new ConnectionFacadeImpl(
            connectionDefinitionService, connectionService, edition, jobPrincipalAccessorRegistry, oAuth2Service,
            tagService, workflowTestConfigurationService, meterRegistryProvider);
    }
}
