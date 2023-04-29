
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
import com.bytechef.atlas.worker.task.handler.TaskHandlerAccessor;
import com.bytechef.hermes.worker.rsocket.client.task.handler.TaskHandlerRSocketClient;
import com.bytechef.hermes.worker.rsocket.client.task.handler.TriggerHandlerRSocketClient;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandlerAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkerRSocketClientConfiguration {

    private final TaskHandlerRSocketClient taskHandlerRSocketClient;
    private final TriggerHandlerRSocketClient triggerHandlerRSocketClient;

    public WorkerRSocketClientConfiguration(
        TaskHandlerRSocketClient taskHandlerRSocketClient, TriggerHandlerRSocketClient triggerHandlerRSocketClient) {

        this.taskHandlerRSocketClient = taskHandlerRSocketClient;
        this.triggerHandlerRSocketClient = triggerHandlerRSocketClient;
    }

    @Bean
    TaskHandlerAccessor taskHandlerAccessor() {
        return type -> (TaskHandler<?>) taskExecution -> taskHandlerRSocketClient.handle(type, taskExecution);
    }

    @Bean
    TriggerHandlerAccessor triggerHandlerAccessor() {
        return type -> (TriggerHandler<?>) triggerExecution -> triggerHandlerRSocketClient.handle(type,
            triggerExecution);
    }
}
