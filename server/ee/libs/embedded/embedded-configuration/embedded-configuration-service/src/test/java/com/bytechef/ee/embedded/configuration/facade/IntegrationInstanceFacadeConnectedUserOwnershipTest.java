/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationInstanceFacadeConnectedUserOwnershipTest {

    private static final long CONNECTED_USER_A_ID = 100L;
    private static final long CONNECTED_USER_B_ID = 200L;
    private static final String EXTERNAL_USER_A_ID = "external-user-a";
    private static final long INTEGRATION_INSTANCE_A_ID = 1L;
    private static final long INTEGRATION_INSTANCE_B_ID = 2L;
    private static final long INTEGRATION_INSTANCE_CONFIGURATION_ID = 9L;
    private static final long INTEGRATION_INSTANCE_WORKFLOW_ID = 5L;
    private static final String WORKFLOW_ID = "workflow-1";

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final IntegrationInstanceWorkflow integrationInstanceWorkflow = mock(IntegrationInstanceWorkflow.class);
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService =
        mock(IntegrationInstanceWorkflowService.class);
    private final PermissionService permissionService = mock(PermissionService.class);

    private IntegrationInstanceFacade integrationInstanceFacade;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getWebhookUrl()).thenReturn("http://localhost/webhooks");

        ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade =
            new ConnectedUserIntegrationInstanceFacadeImpl(
                mock(ComponentDefinitionFacade.class), connectedUserService, integrationInstanceConfigurationService,
                integrationInstanceService, mock(IntegrationInstanceFacade.class),
                mock(IntegrationWorkflowService.class));

        ObjectProvider<ConnectedUserIntegrationInstanceFacade> connectedUserIntegrationInstanceFacadeProvider =
            mock(ObjectProvider.class);

        when(connectedUserIntegrationInstanceFacadeProvider.getObject())
            .thenReturn(connectedUserIntegrationInstanceFacade);

        IntegrationInstanceFacadeImpl integrationInstanceFacadeImpl = new IntegrationInstanceFacadeImpl(
            applicationProperties, connectedUserIntegrationInstanceFacadeProvider, connectedUserService,
            mock(Evaluator.class), integrationInstanceConfigurationService,
            mock(IntegrationInstanceConfigurationWorkflowService.class), integrationInstanceWorkflowService,
            integrationInstanceService, mock(IntegrationService.class), mock(IntegrationWorkflowService.class),
            mock(JobService.class), mock(PrincipalJobService.class), mock(TriggerLifecycleFacade.class),
            mock(ComponentConnectionFacade.class), mock(WorkflowService.class));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(
            new AutomationMethodSecurityExpressionHandler(permissionService));

        ProxyFactory proxyFactory = new ProxyFactory(integrationInstanceFacadeImpl);

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        integrationInstanceFacade = (IntegrationInstanceFacade) proxyFactory.getProxy();

        setUpIntegrationInstance(INTEGRATION_INSTANCE_A_ID, CONNECTED_USER_A_ID);
        setUpIntegrationInstance(INTEGRATION_INSTANCE_B_ID, CONNECTED_USER_B_ID);

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            mock(IntegrationInstanceConfiguration.class);

        when(integrationInstanceConfiguration.getEnvironment()).thenReturn(Environment.PRODUCTION);
        when(integrationInstanceConfiguration.getEnvironmentId()).thenReturn((long) Environment.PRODUCTION.ordinal());
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
            INTEGRATION_INSTANCE_CONFIGURATION_ID))
                .thenReturn(integrationInstanceConfiguration);

        ConnectedUser connectedUserA = mock(ConnectedUser.class);

        when(connectedUserA.getId()).thenReturn(CONNECTED_USER_A_ID);
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_A_ID, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUserA));
        when(connectedUserService.getConnectedUser(anyLong())).thenReturn(mock(ConnectedUser.class));

        when(integrationInstanceWorkflow.getId()).thenReturn(INTEGRATION_INSTANCE_WORKFLOW_ID);
        when(integrationInstanceWorkflowService.fetchIntegrationInstanceWorkflow(anyLong(), anyString()))
            .thenReturn(Optional.of(integrationInstanceWorkflow));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniesConnectedUserOnAnotherUsersInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            INTEGRATION_INSTANCE_B_ID, WORKFLOW_ID, true))
                .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verify(integrationInstanceWorkflowService, never()).updateEnabled(any(), anyBoolean());
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniesConnectedUserFromAnotherEnvironment() {
        authenticate(createConnectedUserAuthentication(Environment.DEVELOPMENT));

        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            INTEGRATION_INSTANCE_A_ID, WORKFLOW_ID, true))
                .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verify(integrationInstanceWorkflowService, never()).updateEnabled(any(), anyBoolean());
    }

    @Test
    void testEnableIntegrationInstanceWorkflowAllowsConnectedUserOnOwnInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(INTEGRATION_INSTANCE_A_ID, WORKFLOW_ID, true);

        verify(integrationInstanceWorkflowService).updateEnabled(INTEGRATION_INSTANCE_WORKFLOW_ID, true);
    }

    @Test
    void testEnableIntegrationInstanceWorkflowAllowsTenantAdminOnAnyInstance() {
        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(INTEGRATION_INSTANCE_B_ID, WORKFLOW_ID, true);

        verify(integrationInstanceWorkflowService).updateEnabled(INTEGRATION_INSTANCE_WORKFLOW_ID, true);
        verify(connectedUserService, never()).fetchConnectedUser(anyString(), any(Environment.class));
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniesCallerWhoIsNeitherTenantAdminNorConnectedUser() {
        authenticate(
            new UsernamePasswordAuthenticationToken(
                "user@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            INTEGRATION_INSTANCE_A_ID, WORKFLOW_ID, true))
                .isInstanceOf(AccessDeniedException.class);

        verify(integrationInstanceWorkflowService, never()).updateEnabled(any(), anyBoolean());
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowDeniesConnectedUserOnAnotherUsersInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

        assertThatThrownBy(() -> integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            INTEGRATION_INSTANCE_B_ID, WORKFLOW_ID, Map.of("key", "value")))
                .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verify(integrationInstanceWorkflowService, never()).update(any());
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowAllowsConnectedUserOnOwnInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            INTEGRATION_INSTANCE_A_ID, WORKFLOW_ID, Map.of("key", "value"));

        verify(integrationInstanceWorkflowService).update(integrationInstanceWorkflow);
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowAllowsTenantAdminOnAnyInstance() {
        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            INTEGRATION_INSTANCE_B_ID, WORKFLOW_ID, Map.of("key", "value"));

        verify(integrationInstanceWorkflowService).update(integrationInstanceWorkflow);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static EmbeddedApiKeyAuthenticationToken createConnectedUserAuthentication(Environment environment) {
        return new EmbeddedApiKeyAuthenticationToken(
            environment.ordinal(), new User(EXTERNAL_USER_A_ID, "", List.of()));
    }

    private Authentication createTenantAdminAuthentication() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        return new UsernamePasswordAuthenticationToken(
            "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private void setUpIntegrationInstance(long integrationInstanceId, long connectedUserId) {
        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectedUserId()).thenReturn(connectedUserId);
        when(integrationInstance.getIntegrationInstanceConfigurationId())
            .thenReturn(INTEGRATION_INSTANCE_CONFIGURATION_ID);
        when(integrationInstanceService.getIntegrationInstance(integrationInstanceId)).thenReturn(integrationInstance);
    }
}
