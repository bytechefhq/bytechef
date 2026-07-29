/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserCodeWorkflowReferenceFacadeClient implements ConnectedUserCodeWorkflowReferenceFacade {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String CONNECTED_USER_CODE_WORKFLOW_REFERENCE_FACADE =
        "/remote/connected-user-code-workflow-reference-facade";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserCodeWorkflowReferenceFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public void deleteReference(String externalUserId, String catalogWorkflowUuid, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableReference(
        String externalUserId, String catalogWorkflowUuid, boolean enable, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProjectWorkflow> getConnectedUserWorkflows(long connectedUserId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    CONNECTED_USER_CODE_WORKFLOW_REFERENCE_FACADE + "/get-connected-user-workflows/"
                        + connectedUserId)
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String catalogWorkflowUuid, Environment environment) {

        try {
            return loadBalancedRestClient.post(
                uriBuilder -> uriBuilder
                    .host(CONFIGURATION_APP)
                    .path(CONNECTED_USER_CODE_WORKFLOW_REFERENCE_FACADE + "/get-or-create-reference")
                    .queryParam("externalUserId", externalUserId)
                    .queryParam("catalogWorkflowUuid", catalogWorkflowUuid)
                    .queryParam("environment", environment)
                    .build(),
                null, ConnectedUserProjectWorkflow.class);
        } catch (HttpClientErrorException.Conflict conflict) {
            Map<String, String> body = conflict.getResponseBodyAs(new ParameterizedTypeReference<>() {});

            throw new MissingConnectionException(body.get("missingConnectionComponentName"));
        }
    }

    @Override
    public void markDanglingReferences(
        long catalogProjectId, Set<String> previousCatalogWorkflowUuids, Set<String> currentCatalogWorkflowUuids) {

        throw new UnsupportedOperationException();
    }
}
