/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAuthenticationResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resolves the connected user who owns a project deployment built in the embedded workflow builder.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConnectedUserProjectJobPrincipalAuthenticationResolver implements JobPrincipalAuthenticationResolver {

    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectJobPrincipalAuthenticationResolver(
        ConnectedUserProjectService connectedUserProjectService, ConnectedUserService connectedUserService) {

        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserService = connectedUserService;
    }

    @Override
    public Optional<Authentication> fetchAuthentication(long jobPrincipalId) {
        return connectedUserProjectService.fetchConnectedUserId(jobPrincipalId)
            .flatMap(connectedUserId -> ConnectedUserAuthentications.fetchAuthentication(
                connectedUserService, connectedUserId));
    }

    @Override
    public PlatformType getType() {
        return PlatformType.AUTOMATION;
    }

    @Override
    public boolean isApplicable(long jobPrincipalId) {
        return connectedUserProjectService.containsProjectDeployment(jobPrincipalId);
    }
}
