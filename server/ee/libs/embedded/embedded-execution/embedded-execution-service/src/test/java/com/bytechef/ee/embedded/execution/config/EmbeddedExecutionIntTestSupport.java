/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.config;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.repository.ConnectedUserRepository;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.tool.invocation.log.repository.ToolInvocationLogRepository;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings("EI")
public class EmbeddedExecutionIntTestSupport {

    private final ConnectedUserRepository connectedUserRepository;
    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceRepository integrationInstanceRepository;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationRepository integrationRepository;
    private final IntegrationService integrationService;
    private final ToolInvocationLogRepository toolInvocationLogRepository;

    public EmbeddedExecutionIntTestSupport(
        ConnectedUserRepository connectedUserRepository, ConnectedUserService connectedUserService,
        IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceRepository integrationInstanceRepository,
        IntegrationInstanceService integrationInstanceService, IntegrationRepository integrationRepository,
        IntegrationService integrationService, ToolInvocationLogRepository toolInvocationLogRepository) {

        this.connectedUserRepository = connectedUserRepository;
        this.connectedUserService = connectedUserService;
        this.integrationInstanceConfigurationRepository = integrationInstanceConfigurationRepository;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceRepository = integrationInstanceRepository;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationRepository = integrationRepository;
        this.integrationService = integrationService;
        this.toolInvocationLogRepository = toolInvocationLogRepository;
    }

    public long createConnectedUser(String externalUserId, Environment environment) {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(externalUserId, environment);

        return Objects.requireNonNull(connectedUser.getId());
    }

    public IntegrationInstance createIntegrationInstance(
        long connectedUserId, String componentName, Environment environment, long connectionId) {

        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        return integrationInstanceService.create(
            connectedUserId, connectionId, Objects.requireNonNull(integrationInstanceConfiguration.getId()));
    }

    public void deleteAll() {
        toolInvocationLogRepository.deleteAll();
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
        connectedUserRepository.deleteAll();
    }
}
