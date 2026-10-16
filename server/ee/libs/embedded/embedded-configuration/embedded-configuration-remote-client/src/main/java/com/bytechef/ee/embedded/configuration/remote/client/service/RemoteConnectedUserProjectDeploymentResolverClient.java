/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserProjectDeploymentResolverClient implements ConnectedUserProjectDeploymentResolver {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String CONNECTED_USER_PROJECT_DEPLOYMENT_RESOLVER =
        "/remote/connected-user-project-deployment-resolver";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserProjectDeploymentResolverClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public List<Long> getProjectDeploymentIds(long connectedUserId) {
        List<Long> projectDeploymentIds = loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(CONNECTED_USER_PROJECT_DEPLOYMENT_RESOLVER + "/get-project-deployment-ids/{connectedUserId}")
                .build(connectedUserId),
            new ParameterizedTypeReference<>() {});

        return projectDeploymentIds == null ? List.of() : projectDeploymentIds;
    }
}
