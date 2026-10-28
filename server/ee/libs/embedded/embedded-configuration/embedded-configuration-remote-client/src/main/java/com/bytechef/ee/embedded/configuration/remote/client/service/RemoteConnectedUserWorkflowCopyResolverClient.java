/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.automation.configuration.service.ConnectedUserWorkflowCopyResolver;
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
public class RemoteConnectedUserWorkflowCopyResolverClient implements ConnectedUserWorkflowCopyResolver {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String CONNECTED_USER_WORKFLOW_COPY_RESOLVER = "/remote/connected-user-workflow-copy-resolver";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserWorkflowCopyResolverClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public List<String> getWorkflowCopyUuids(String workflowUuid) {
        List<String> workflowCopyUuids = loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(CONNECTED_USER_WORKFLOW_COPY_RESOLVER + "/get-workflow-copy-uuids/{workflowUuid}")
                .build(workflowUuid),
            new ParameterizedTypeReference<>() {});

        return workflowCopyUuids == null ? List.of() : workflowCopyUuids;
    }
}
