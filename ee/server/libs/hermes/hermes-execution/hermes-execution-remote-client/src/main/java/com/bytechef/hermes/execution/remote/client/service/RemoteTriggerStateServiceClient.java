/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.execution.remote.client.service;

import com.bytechef.commons.restclient.LoadBalancedRestClient;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("triggerStorageService")
public class RemoteTriggerStateServiceClient implements TriggerStateService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String TRIGGER_STORAGE_SERVICE = "/remote/trigger-state-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteTriggerStateServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public <T> Optional<T> fetchValue(WorkflowExecutionId workflowExecutionId) {
        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(EXECUTION_APP)
                    .path(TRIGGER_STORAGE_SERVICE + "/fetch-value/{workflowExecutionId}")
                    .build(workflowExecutionId),
                new ParameterizedTypeReference<T>() {}));
    }

    @Override
    public void save(WorkflowExecutionId workflowExecutionId, Object value) {
        loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TRIGGER_STORAGE_SERVICE + "/save/{workflowExecutionId}")
                .build(workflowExecutionId),
            value);
    }
}
