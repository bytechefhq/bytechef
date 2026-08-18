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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
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
import java.util.Map;
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

    @Test
    void testUpdateAuthorizationNonOAuthUpdatesParametersDirectly() {
        ConnectionFacadeImpl facade = newFacade("EE");

        Connection connection = new Connection();

        connection.setId(5L);
        connection.setComponentName("dummy");

        when(connectionService.getConnection(5L)).thenReturn(connection);

        facade.updateAuthorization(5L, Map.of("apiKey", "new"));

        verify(connectionService).updateConnectionParameters(5L, Map.of("apiKey", "new"));
    }

    @Test
    void testUpdateAuthorizationOAuth2AuthorizationCodeMergesCallbackResult() {
        ConnectionFacadeImpl facade = newFacade("EE");

        Connection connection = new Connection();

        connection.setId(5L);
        connection.setComponentName("slack");
        connection.setConnectionVersion(1);
        connection.setAuthorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
        connection.setParameters(Map.of(Authorization.CLIENT_ID, "id", Authorization.CLIENT_SECRET, "secret"));

        when(connectionService.getConnection(5L)).thenReturn(connection);
        when(connectionDefinitionService.getAuthorizationType("slack", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
            .thenReturn(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
        when(oAuth2Service.checkPredefinedParameters(eq("slack"), any())).thenReturn(Map.of());
        when(oAuth2Service.getRedirectUri()).thenReturn("http://localhost/callback");
        when(connectionDefinitionService.executeAuthorizationCallback(
            eq("slack"), eq(1), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE), any(),
            eq("http://localhost/callback")))
                .thenReturn(new AuthorizationCallbackResponse(Map.of(Authorization.ACCESS_TOKEN, "tok123")));

        facade.updateAuthorization(5L, Map.of(Authorization.CODE, "auth-code"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.captor();

        verify(connectionDefinitionService).executeAuthorizationCallback(
            eq("slack"), eq(1), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE), any(),
            eq("http://localhost/callback"));
        verify(connectionService).updateConnectionParameters(eq(5L), captor.capture());

        assertThat(captor.getValue()).containsEntry(Authorization.ACCESS_TOKEN, "tok123");
    }

    @Test
    void testUpdateAuthorizationStripsStateParameter() {
        ConnectionFacadeImpl facade = newFacade("EE");

        Connection connection = new Connection();

        connection.setId(5L);
        connection.setComponentName("dummy");

        when(connectionService.getConnection(5L)).thenReturn(connection);

        facade.updateAuthorization(5L, Map.of("apiKey", "new", "state", "csrf-token"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.captor();

        verify(connectionService).updateConnectionParameters(eq(5L), captor.capture());

        assertThat(captor.getValue())
            .containsEntry("apiKey", "new")
            .doesNotContainKey("state");
    }

    private ConnectionFacadeImpl newFacade(String edition) {
        return new ConnectionFacadeImpl(
            connectionDefinitionService, connectionService, edition, jobPrincipalAccessorRegistry, oAuth2Service,
            tagService, workflowTestConfigurationService, meterRegistryProvider);
    }
}
