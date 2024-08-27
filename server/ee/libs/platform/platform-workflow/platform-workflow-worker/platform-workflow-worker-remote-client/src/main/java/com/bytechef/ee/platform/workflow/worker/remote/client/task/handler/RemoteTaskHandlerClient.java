/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.client.task.handler;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTaskHandlerClient {

    private static final String WORKER_APP = "worker-app";
    private static final String TASK_HANDLER = "/remote/task-handler";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteTaskHandlerClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    public Object handle(String type, TaskExecution taskExecution) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(WORKER_APP)
                .path(
                    TASK_HANDLER + "/handle/{type}")
                .build(type),
            taskExecution,
            new ParameterizedTypeReference<>() {});
    }
}
