/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Fixtures for the tests that use {@link TriggerSubscriptionLifecycleIntTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class TriggerSubscriptionLifecycleTestSupport {

    public static final String WEBHOOK_COMPONENT_NAME = "webhook";
    public static final String WEBHOOK_TRIGGER_NAME = "autoRespondWithHTTP200";

    private static final String CONNECTION_FOREIGN_KEY_NAME = "fk_project_deployment_workflow_connection_connection";

    private TriggerSubscriptionLifecycleTestSupport() {
    }

    public static void addConnectionForeignKey(JdbcClient jdbcClient) {
        dropConnectionForeignKey(jdbcClient);

        jdbcClient
            .sql(
                "ALTER TABLE project_deployment_workflow_connection ADD CONSTRAINT " + CONNECTION_FOREIGN_KEY_NAME +
                    " FOREIGN KEY (connection_id) REFERENCES connection (id) NOT VALID")
            .update();
    }

    public static void authenticateAdmin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
    }

    public static long createConnection(ConnectionService connectionService) {
        return createConnection(connectionService, null);
    }

    public static long createConnection(
        ConnectionService connectionService, @Nullable AuthorizationType authorizationType) {

        Connection connection = new Connection();

        connection.setAuthorizationType(authorizationType);

        connection.setComponentName(WEBHOOK_COMPONENT_NAME);
        connection.setConnectionVersion(1);
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setName("trigger-subscription-connection-" + UUID.randomUUID());
        connection.setParameters(Map.of());
        connection.setType(PlatformType.EMBEDDED);

        connection = connectionService.create(connection);

        return Validate.notNull(connection.getId(), "id");
    }

    public static long createIntegrationInstance(
        IntegrationInstanceService integrationInstanceService, IntegrationRepository integrationRepository,
        JdbcClient jdbcClient, long connectedUserId, long connectionId) {

        Integration integration = new Integration();

        integration.setComponentName(WEBHOOK_COMPONENT_NAME);
        integration.setName("Trigger Subscription Integration " + UUID.randomUUID());

        integration = integrationRepository.save(integration);

        long integrationInstanceConfigurationId = jdbcClient
            .sql(
                """
                    INSERT INTO integration_instance_configuration (
                        integration_id, integration_version, name, enabled, environment, authorization_type,
                        connection_parameters, created_by, created_date, last_modified_by, last_modified_date,
                        version)
                    VALUES (:integrationId, 1, 'Webhook', true, :environment, 0, '{}', 'system', now(), 'system',
                        now(), 0)
                    RETURNING id
                    """)
            .param("integrationId", integration.getId())
            .param("environment", Environment.PRODUCTION.ordinal())
            .query(Long.class)
            .single();

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            connectedUserId, connectionId, integrationInstanceConfigurationId);

        return Validate.notNull(integrationInstance.getId(), "id");
    }

    public static void dropConnectionForeignKey(JdbcClient jdbcClient) {
        jdbcClient
            .sql(
                "ALTER TABLE project_deployment_workflow_connection DROP CONSTRAINT IF EXISTS " +
                    CONNECTION_FOREIGN_KEY_NAME)
            .update();
    }

    public static String getWorkflowId(
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, TriggerSubscription triggerSubscription) {

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                Validate.notNull(triggerSubscription.getProjectDeploymentId(), "projectDeploymentId"));

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflows.getFirst();

        return projectDeploymentWorkflow.getWorkflowId();
    }

    public static void stubCollaborators(
        ConnectionDefinitionService connectionDefinitionService,
        EmbeddedPermissionEvaluator embeddedPermissionEvaluator,
        EnvironmentService environmentService, OAuth2Service oAuth2Service, PrincipalJobService principalJobService,
        TriggerDefinitionService triggerDefinitionService) {

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getName()).thenReturn(WEBHOOK_TRIGGER_NAME);
        when(triggerDefinition.getType()).thenReturn(TriggerType.STATIC_WEBHOOK);

        when(triggerDefinitionService.getTriggerDefinition(WEBHOOK_COMPONENT_NAME, 1, WEBHOOK_TRIGGER_NAME))
            .thenReturn(triggerDefinition);

        ConnectionDefinition connectionDefinition = mock(ConnectionDefinition.class);

        when(connectionDefinition.getAuthorizations()).thenReturn(List.of());
        when(connectionDefinition.getProperties()).thenReturn(List.of());

        when(connectionDefinitionService.getConnectionConnectionDefinition(WEBHOOK_COMPONENT_NAME, 1))
            .thenReturn(connectionDefinition);
        when(connectionDefinitionService.executeBaseUri(anyString(), any()))
            .thenReturn(Optional.empty());
        when(oAuth2Service.checkPredefinedParameters(anyString(), anyMap()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(environmentService.getEnvironment(Environment.PRODUCTION.ordinal()))
            .thenReturn(Environment.PRODUCTION);
        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                .thenReturn(Page.empty());
        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(true);
    }
}
