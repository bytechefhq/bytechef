/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.workflow.execution.remote.client.service;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
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
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteJobTriggerExecution(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TriggerExecution> fetchJobTriggerExecution(long jobId) {
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
