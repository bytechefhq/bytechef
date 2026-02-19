/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.worker.config;

import com.bytechef.atlas.worker.annotation.ConditionalOnWorker;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.worker.task.SseStreamTaskExecutionPostOutputProcessor;
import com.bytechef.platform.worker.task.SuspendTaskExecutionPostOutputProcessor;
import com.bytechef.platform.worker.task.WebhookResponseTaskExecutionPostOutputProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnWorker
public class PlatformWorkerConfiguration {

    @Bean
    SseStreamTaskExecutionPostOutputProcessor sseStreamTaskExecutionPostOutputProcessor(MessageBroker messageBroker) {
        return new SseStreamTaskExecutionPostOutputProcessor(messageBroker);
    }

    @Bean
    SuspendTaskExecutionPostOutputProcessor suspendTaskExecutionPostOutputProcessor(TriggerScheduler triggerScheduler) {
        return new SuspendTaskExecutionPostOutputProcessor(triggerScheduler);
    }

    @Bean
    WebhookResponseTaskExecutionPostOutputProcessor webhookResponseTaskExecutionPostOutputProcessor() {
        return new WebhookResponseTaskExecutionPostOutputProcessor();
    }
}
