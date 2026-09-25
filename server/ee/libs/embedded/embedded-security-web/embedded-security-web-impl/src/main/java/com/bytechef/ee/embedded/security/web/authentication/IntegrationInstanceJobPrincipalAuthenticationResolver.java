/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAuthenticationResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resolves the connected user who owns an integration instance.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationInstanceJobPrincipalAuthenticationResolver implements JobPrincipalAuthenticationResolver {

    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceJobPrincipalAuthenticationResolver(
        ConnectedUserService connectedUserService, IntegrationInstanceService integrationInstanceService) {

        this.connectedUserService = connectedUserService;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    public Optional<Authentication> fetchAuthentication(long jobPrincipalId) {
        List<IntegrationInstance> integrationInstances = integrationInstanceService.getIntegrationInstances(
            List.of(jobPrincipalId));

        return integrationInstances.stream()
            .findFirst()
            .map(IntegrationInstance::getConnectedUserId)
            .flatMap(connectedUserId -> ConnectedUserAuthentications.fetchAuthentication(
                connectedUserService, connectedUserId));
    }

    @Override
    public PlatformType getType() {
        return PlatformType.EMBEDDED;
    }
}
