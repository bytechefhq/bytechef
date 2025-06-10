/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectedUserIntegrationInstanceFacadeImpl implements ConnectedUserIntegrationInstanceFacade {

    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceFacade integrationInstanceFacade;
    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserIntegrationInstanceFacadeImpl(
        ConnectedUserService connectedUserService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService, IntegrationInstanceFacade integrationInstanceFacade,
        IntegrationWorkflowService integrationWorkflowService) {

        this.connectedUserService = connectedUserService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceFacade = integrationInstanceFacade;
        this.integrationWorkflowService = integrationWorkflowService;
    }

    @Override
    public void disableIntegrationInstanceWorkflow(String externalUserId, long id, String workflowReferenceCode) {
        enableIntegrationInstanceWorkflow(externalUserId, id, workflowReferenceCode, false);
    }

    @Override
    public void enableIntegrationInstanceWorkflow(String externalUserId, long id, String workflowReferenceCode) {
        enableIntegrationInstanceWorkflow(externalUserId, id, workflowReferenceCode, true);
    }

    @Override
    public void updateIntegrationInstanceWorkflow(
        String externalUserId, long id, String workflowReferenceCode, Map<String, Object> inputs) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        connectedUserService.fetchConnectedUser(externalUserId, integrationInstanceConfiguration.getEnvironment())
            .ifPresent(connectedUser -> {
                if (Objects.equals(connectedUser.getExternalId(), externalUserId)) {
                    integrationInstanceFacade.updateIntegrationInstanceWorkflow(
                        id, integrationWorkflowService.getWorkflowId(id, workflowReferenceCode), inputs);
                }
            });
    }

    private void enableIntegrationInstanceWorkflow(
        String externalUserId, long id, String workflowReferenceCode, boolean enabled) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        connectedUserService.fetchConnectedUser(externalUserId, integrationInstanceConfiguration.getEnvironment())
            .ifPresent(connectedUser -> {
                if (Objects.equals(connectedUser.getExternalId(), externalUserId)) {
                    integrationInstanceFacade.enableIntegrationInstanceWorkflow(
                        id, integrationWorkflowService.getWorkflowId(id, workflowReferenceCode), enabled);
                }
            });
    }
}
