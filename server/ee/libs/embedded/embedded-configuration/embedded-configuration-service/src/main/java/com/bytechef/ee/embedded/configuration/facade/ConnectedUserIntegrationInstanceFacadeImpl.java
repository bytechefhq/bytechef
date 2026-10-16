/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentications;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ConnectedUserIntegrationInstanceFacadeImpl implements ConnectedUserIntegrationInstanceFacade {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserIntegrationInstanceFacadeImpl.class);

    private final ComponentDefinitionFacade componentDefinitionFacade;
    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceFacade integrationInstanceFacade;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserIntegrationInstanceFacadeImpl(
        ComponentDefinitionFacade componentDefinitionFacade, ConnectedUserService connectedUserService,
        ConnectionService connectionService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService, IntegrationInstanceFacade integrationInstanceFacade,
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService) {

        this.componentDefinitionFacade = componentDefinitionFacade;
        this.connectedUserService = connectedUserService;
        this.connectionService = connectionService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceFacade = integrationInstanceFacade;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
    }

    @Override
    public void disableIntegrationInstanceWorkflow(String externalUserId, long id, String workflowUuid) {
        enableIntegrationInstanceWorkflow(externalUserId, id, workflowUuid, false);
    }

    @Override
    public void enableIntegrationInstanceWorkflow(String externalUserId, long id, String workflowUuid) {
        enableIntegrationInstanceWorkflow(externalUserId, id, workflowUuid, true);
    }

    @Override
    public List<Option> getComponentInputOptions(
        String externalUserId, long id, String componentName, int componentVersion, String groupName,
        String propertyName, Map<String, Object> lookupDependsOnValues, String searchText) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        if (!isOwnedByConnectedUser(externalUserId, id, integrationInstance)) {
            return List.of();
        }

        Optional<Long> connectionId = findComponentConnectionId(integrationInstance, componentName);

        if (connectionId.isEmpty()) {
            return List.of();
        }

        return componentDefinitionFacade.executeWorkflowInputOptions(
            componentName, componentVersion, groupName, propertyName, lookupDependsOnValues,
            List.copyOf(lookupDependsOnValues.keySet()), searchText, connectionId.get());
    }

    private Optional<Long> findComponentConnectionId(IntegrationInstance integrationInstance, String componentName) {
        long integrationInstanceId = Objects.requireNonNull(integrationInstance.getId());

        Integration integration = integrationService.getIntegrationInstanceIntegration(integrationInstanceId);

        if (Objects.equals(integration.getComponentName(), componentName)) {
            return Optional.of(integrationInstance.getConnectionId());
        }

        List<Long> configuredConnectionIds = integrationInstanceConfigurationWorkflowService
            .getIntegrationInstanceConfigurationWorkflows(integrationInstance.getIntegrationInstanceConfigurationId())
            .stream()
            .flatMap(integrationInstanceConfigurationWorkflow -> integrationInstanceConfigurationWorkflow
                .getConnections()
                .stream())
            .map(IntegrationInstanceConfigurationWorkflowConnection::getConnectionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (configuredConnectionIds.isEmpty()) {
            return Optional.empty();
        }

        return connectionService.getConnections(configuredConnectionIds)
            .stream()
            .filter(connection -> Objects.equals(connection.getComponentName(), componentName))
            .map(Connection::getId)
            .findFirst();
    }

    @Override
    public void updateIntegrationInstanceWorkflow(
        String externalUserId, long id, String workflowUuid, Map<String, Object> inputs) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        if (!isOwnedByConnectedUser(externalUserId, id, integrationInstance)) {
            throw new EmbeddedIntegrationNotVisibleException(id);
        }

        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            id, integrationWorkflowService.getWorkflowId(id, workflowUuid), inputs);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCurrentPrincipalIntegrationInstanceOwnership(long id) {
        if (!ConnectedUserAuthentications.isConnectedUser()) {
            return;
        }

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        Optional<String> externalUserId = SecurityUtils.fetchCurrentUserLogin();
        Optional<Long> principalEnvironmentId = fetchCurrentPrincipalEnvironmentId();

        if (externalUserId.isEmpty() || principalEnvironmentId.isEmpty() ||
            principalEnvironmentId.get() != integrationInstanceConfiguration.getEnvironmentId() ||
            !isOwnedByConnectedUser(
                externalUserId.get(), id, integrationInstance, integrationInstanceConfiguration)) {

            throw new EmbeddedIntegrationNotVisibleException(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateIntegrationInstanceOwnership(String externalUserId, long id) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        if (!isOwnedByConnectedUser(externalUserId, id, integrationInstance)) {
            throw new EmbeddedIntegrationNotVisibleException(id);
        }
    }

    private void enableIntegrationInstanceWorkflow(
        String externalUserId, long id, String workflowUuid, boolean enable) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        if (!isOwnedByConnectedUser(externalUserId, id, integrationInstance)) {
            throw new EmbeddedIntegrationNotVisibleException(id);
        }

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            id, integrationWorkflowService.getWorkflowId(id, workflowUuid), enable);
    }

    public static Optional<Long> fetchCurrentPrincipalEnvironmentId() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (!(authentication instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken)) {
            return Optional.empty();
        }

        return Optional.ofNullable(
            apiKeyAuthenticationToken.getEnvironmentId() == -1 ? null : apiKeyAuthenticationToken.getEnvironmentId());
    }

    private boolean isOwnedByConnectedUser(String externalUserId, long id, IntegrationInstance integrationInstance) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        return isOwnedByConnectedUser(externalUserId, id, integrationInstance, integrationInstanceConfiguration);
    }

    private boolean isOwnedByConnectedUser(
        String externalUserId, long id, IntegrationInstance integrationInstance,
        IntegrationInstanceConfiguration integrationInstanceConfiguration) {

        Long owningConnectedUserId = integrationInstance.getConnectedUserId();

        Long resolvedConnectedUserId = connectedUserService
            .fetchConnectedUser(externalUserId, integrationInstanceConfiguration.getEnvironment())
            .map(ConnectedUser::getId)
            .orElse(null);

        boolean owned =
            resolvedConnectedUserId != null && Objects.equals(owningConnectedUserId, resolvedConnectedUserId);

        if (!owned && log.isWarnEnabled()) {
            log.warn(
                "Denying access to integration instance {}: owning connected-user id {} does not match the resolved " +
                    "connected-user id {} for the request.",
                id, owningConnectedUserId, resolvedConnectedUserId);
        }

        return owned;
    }
}
