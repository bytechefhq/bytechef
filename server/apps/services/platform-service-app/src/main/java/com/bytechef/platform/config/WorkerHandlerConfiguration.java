
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.config;

import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.hermes.worker.handler.remote.web.rest.client.TaskHandlerClient;
import com.bytechef.hermes.worker.handler.remote.web.rest.client.TriggerHandlerClient;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandlerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkerHandlerConfiguration {

    private final TaskHandlerClient taskHandlerClient;
    private final TriggerHandlerClient triggerHandlerClient;

    public WorkerHandlerConfiguration(TaskHandlerClient taskHandlerClient, TriggerHandlerClient triggerHandlerClient) {
        this.taskHandlerClient = taskHandlerClient;
        this.triggerHandlerClient = triggerHandlerClient;
    }

    @Bean
    TaskHandlerRegistry taskHandlerAccessor() {
        return type -> (TaskHandler<?>) taskExecution -> taskHandlerClient.handle(type, taskExecution);
    }

    @Bean
    TriggerHandlerRegistry triggerHandlerAccessor() {
        return type -> (TriggerHandler<?>) triggerExecution -> triggerHandlerClient.handle(type, triggerExecution);
    }
}
