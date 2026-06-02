/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.Option;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final ConnectedUserService connectedUserService;
    private final EmbeddedWorkflowInputOptionFacade embeddedWorkflowInputOptionFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceFacade integrationInstanceFacade;
    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserIntegrationInstanceFacadeImpl(
        ConnectedUserService connectedUserService,
        EmbeddedWorkflowInputOptionFacade embeddedWorkflowInputOptionFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService, IntegrationInstanceFacade integrationInstanceFacade,
        IntegrationWorkflowService integrationWorkflowService) {

        this.connectedUserService = connectedUserService;
        this.embeddedWorkflowInputOptionFacade = embeddedWorkflowInputOptionFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceFacade = integrationInstanceFacade;
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
    public List<Option> getIntegrationInstanceWorkflowInputOptions(
        String externalUserId, long id, String workflowUuid, String inputName, String propertyName,
        Map<String, Object> lookupDependsOnValues, String searchText) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        // Return no options when the integration instance does not belong to the connected user, instead of
        // leaking instance existence. Reads use the anti-enumeration empty result; the mutating paths fail closed
        // with EmbeddedIntegrationNotVisibleException (HTTP 404) so the caller is not told the change was a no-op.
        if (!isOwnedByConnectedUser(externalUserId, id, integrationInstance)) {
            return List.of();
        }

        return embeddedWorkflowInputOptionFacade.getWorkflowInputOptions(
            id, workflowUuid, inputName, propertyName, lookupDependsOnValues, searchText);
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

    private void enableIntegrationInstanceWorkflow(
        String externalUserId, long id, String workflowUuid, boolean enable) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        if (!isOwnedByConnectedUser(externalUserId, id, integrationInstance)) {
            throw new EmbeddedIntegrationNotVisibleException(id);
        }

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            id, integrationWorkflowService.getWorkflowId(id, workflowUuid), enable);
    }

    /**
     * Resolves the connected user for the request and verifies it owns the given integration instance. Fails closed: an
     * absent connected user (no row for the instance's environment) is treated as not-owned, and the null/null id case
     * never grants access. Denials are logged with the instance id plus owning and resolved connected-user ids.
     */
    private boolean isOwnedByConnectedUser(String externalUserId, long id, IntegrationInstance integrationInstance) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

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
