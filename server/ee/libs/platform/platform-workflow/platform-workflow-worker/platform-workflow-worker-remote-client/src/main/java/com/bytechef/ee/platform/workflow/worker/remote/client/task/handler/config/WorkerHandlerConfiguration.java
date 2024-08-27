/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.client.task.handler.config;

import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.ee.platform.workflow.worker.remote.client.task.handler.RemoteTaskHandlerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class WorkerHandlerConfiguration {

    @Bean
    TaskHandlerRegistry taskHandlerRegistry(RemoteTaskHandlerClient remoteTaskHandlerClient) {
        return type -> (taskExecution) -> remoteTaskHandlerClient.handle(type, taskExecution);
    }
}
