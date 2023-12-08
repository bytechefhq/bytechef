/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.execution.remote.client.service;

import com.bytechef.commons.restclient.LoadBalancedRestClient;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerExecutionServiceClient implements TriggerExecutionService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String TRIGGER_EXECUTION_SERVICE = "/remote/trigger-execution-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteTriggerExecutionServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public TriggerExecution create(TriggerExecution triggerExecution) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TRIGGER_EXECUTION_SERVICE + "/create")
                .build(),
            triggerExecution, TriggerExecution.class);
    }

    @Override
    public TriggerExecution getJobTriggerExecution(long jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerExecution getTriggerExecution(long id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TRIGGER_EXECUTION_SERVICE + "/get-trigger-execution/{id}")
                .build(id),
            TriggerExecution.class);
    }

    @Override
    public TriggerExecution update(TriggerExecution triggerExecution) {
        return loadBalancedRestClient.put(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(TRIGGER_EXECUTION_SERVICE + "/update")
                .build(),
            triggerExecution, TriggerExecution.class);
    }
}
