/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.dto.ProvisionIntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@PreAuthorize("isTenantAdmin()")
public class IntegrationInstanceAdminFacadeImpl implements IntegrationInstanceAdminFacade {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade;
    private final ConnectedUserIntegrationFacade connectedUserIntegrationFacade;
    private final IntegrationInstanceCredentialsUpdater integrationInstanceCredentialsUpdater;
    private final IntegrationInstanceFacade integrationInstanceFacade;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceAdminFacadeImpl(
        ConnectedUserConnectionFacade connectedUserConnectionFacade,
        ConnectedUserIntegrationFacade connectedUserIntegrationFacade,
        IntegrationInstanceCredentialsUpdater integrationInstanceCredentialsUpdater,
        IntegrationInstanceFacade integrationInstanceFacade, IntegrationInstanceService integrationInstanceService) {

        this.connectedUserConnectionFacade = connectedUserConnectionFacade;
        this.connectedUserIntegrationFacade = connectedUserIntegrationFacade;
        this.integrationInstanceCredentialsUpdater = integrationInstanceCredentialsUpdater;
        this.integrationInstanceFacade = integrationInstanceFacade;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    public void enableIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId, boolean enable) {
        integrationInstanceFacade.enableIntegrationInstanceWorkflow(integrationInstanceId, workflowId, enable);
    }

    @Override
    public ConnectionDTO getIntegrationInstanceConnection(long integrationInstanceId) {
        IntegrationInstance integrationInstance = requireProvisioned(integrationInstanceId);

        long connectionId = integrationInstance.getConnectionId();

        return connectedUserConnectionFacade
            .getConnectedUserConnectionsForAdmin(
                integrationInstance.getConnectedUserId(), null, List.of(connectionId))
            .stream()
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException(
                "Integration instance id=%s not found".formatted(integrationInstanceId)));
    }

    @Override
    public List<ProvisionableIntegrationDTO> getProvisionableIntegrations(long connectedUserId) {
        return connectedUserIntegrationFacade.getProvisionableIntegrations(connectedUserId);
    }

    @Override
    public long provisionIntegrationInstance(
        long connectedUserId, long integrationInstanceConfigurationId, Map<String, Object> connectionParameters,
        List<ProvisionIntegrationInstanceWorkflowDTO> workflows) {

        return connectedUserIntegrationFacade.provisionIntegrationInstance(
            connectedUserId, integrationInstanceConfigurationId, connectionParameters, workflows,
            SecurityUtils.getCurrentUserLogin());
    }

    @Override
    public void updateIntegrationInstanceCredentials(
        long integrationInstanceId, Map<String, ?> parameters, int version) {

        IntegrationInstance integrationInstance = requireProvisioned(integrationInstanceId);

        integrationInstanceCredentialsUpdater.updateCredentials(
            integrationInstanceId, integrationInstance.getConnectedUserId(), integrationInstance.getConnectionId(),
            parameters, version);
    }

    private IntegrationInstance requireProvisioned(long integrationInstanceId) {
        Optional<IntegrationInstance> integrationInstanceOptional = integrationInstanceService.fetchIntegrationInstance(
            integrationInstanceId);

        return integrationInstanceOptional
            .filter(integrationInstance -> integrationInstance.getProvisionedBy() != null)
            .orElseThrow(() -> new NoSuchElementException(
                "Integration instance id=%s not found".formatted(integrationInstanceId)));
    }
}
