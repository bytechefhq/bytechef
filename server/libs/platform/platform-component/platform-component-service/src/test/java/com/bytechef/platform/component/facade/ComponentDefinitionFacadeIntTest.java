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

import static com.bytechef.platform.component.facade.FacadeTestComponentHandler.CHANNEL_ID;
import static com.bytechef.platform.component.facade.FacadeTestComponentHandler.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.config.ComponentRegistryConfiguration;
import com.bytechef.platform.component.config.ComponentRegistryConfigurationSharedMocks;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = {
    ComponentRegistryConfiguration.class, FacadeTestComponentHandler.class
})
@ComponentRegistryConfigurationSharedMocks
class ComponentDefinitionFacadeIntTest {

    @Autowired
    private ComponentDefinitionFacade componentDefinitionFacade;

    @Autowired
    private ConnectionRepository connectionRepository;

    @AfterEach
    void afterEach() {
        connectionRepository.deleteAll();
    }

    @Test
    void testExecuteWorkflowInputOptionsResolvesActiveConnectionAndDelegates() {
        Connection connection = saveConnection("C1", CredentialStatus.VALID);

        List<Option> options = componentDefinitionFacade.executeWorkflowInputOptions(
            COMPONENT_NAME, 1, "channel", "channel", Map.of(), List.of(), null, connection.getId());

        assertThat(options)
            .extracting(Option::getValue)
            .containsExactly("C1");
    }

    @Test
    void testExecuteWorkflowInputOptionsNullConnectionPassesNull() {
        saveConnection("C1", CredentialStatus.VALID);

        List<Option> options = componentDefinitionFacade.executeWorkflowInputOptions(
            COMPONENT_NAME, 1, "channel", "channel", Map.of(), List.of(), null, null);

        assertThat(options).isEmpty();
    }

    @Test
    void testExecuteWorkflowInputOptionsInactiveConnectionThrows() {
        Connection connection = saveConnection("C1", CredentialStatus.INVALID);

        long connectionId = connection.getId();

        assertThatThrownBy(() -> componentDefinitionFacade.executeWorkflowInputOptions(
            COMPONENT_NAME, 1, "channel", "channel", Map.of(), List.of(), null, connectionId))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Facade Test Connection")
                .hasMessageContaining("INVALID");
    }

    @Nested
    class PreAuthorizeEnforcement {

        private ComponentDefinitionFacade securedFacade;

        @BeforeEach
        void beforeEach() {
            ProxyFactory proxyFactory = new ProxyFactory(componentDefinitionFacade);

            proxyFactory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());

            securedFacade = (ComponentDefinitionFacade) proxyFactory.getProxy();
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testDefinitionReadsRequireAuthentication() {
            assertThatThrownBy(() -> securedFacade.getComponentDefinitions())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            assertThatThrownBy(() -> securedFacade.getComponentDefinition(COMPONENT_NAME, 1))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            assertThatThrownBy(() -> securedFacade.fetchComponentDefinition(COMPONENT_NAME, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        }

        @Test
        void testAuthenticatedCallerReachesTheService() {
            SecurityContext securityContext = SecurityContextHolder.getContext();

            securityContext.setAuthentication(new TestingAuthenticationToken("user", "n/a", "ROLE_USER"));

            assertThat(securedFacade.getComponentDefinitions())
                .extracting(ComponentDefinition::getName)
                .contains(COMPONENT_NAME);

            ComponentDefinition componentDefinition = securedFacade.getComponentDefinition(COMPONENT_NAME, 1);

            assertThat(componentDefinition.getName()).isEqualTo(COMPONENT_NAME);
        }
    }

    private Connection saveConnection(String channelId, CredentialStatus credentialStatus) {
        Connection connection = Connection.builder()
            .authorizationType(AuthorizationType.API_KEY)
            .componentName(COMPONENT_NAME)
            .connectionVersion(1)
            .name("Facade Test Connection")
            .parameters(new HashMap<>(Map.of(CHANNEL_ID, channelId)))
            .type(PlatformType.AUTOMATION)
            .build();

        connection.setCredentialStatus(credentialStatus);

        return connectionRepository.save(connection);
    }
}
