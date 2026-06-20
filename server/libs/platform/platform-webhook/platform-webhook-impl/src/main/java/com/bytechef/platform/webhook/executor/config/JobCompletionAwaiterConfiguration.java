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

package com.bytechef.platform.webhook.executor.config;

import static com.bytechef.tenant.TenantContext.CURRENT_TENANT_ID;

import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.executor.JobCompletionAwaiterImpl;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires {@link JobCompletionAwaiterImpl} to the {@code SSE_STREAM_EVENTS} broker route so its per-job futures are
 * completed by the same broker-published job-status signal the SSE streaming endpoint consumes. Like
 * {@code SseStreamMessageBrokerConfigurerConfiguration}, this is the consumer side of the route: it must run wherever
 * {@code await} is called (the node serving the webhook request), which in a distributed topology is not the
 * coordinator, so the listener is not coordinator-gated.
 *
 * @author Ivica Cardic
 */
@Configuration
public class JobCompletionAwaiterConfiguration {

    @Bean
    JobCompletionAwaiterImpl jobCompletionAwaiter(JobService jobService) {
        return new JobCompletionAwaiterImpl(jobService);
    }

    @Bean
    MessageBrokerConfigurer<?> jobCompletionAwaiterMessageBrokerConfigurer(
        JobCompletionAwaiterImpl jobCompletionAwaiter) {

        JobCompletionAwaiterDelegate delegate = new JobCompletionAwaiterDelegate(jobCompletionAwaiter);

        return (listenerEndpointRegistrar, messageBrokerListenerRegistrar) -> messageBrokerListenerRegistrar
            .registerListenerEndpoint(
                listenerEndpointRegistrar, SseStreamMessageRoute.SSE_STREAM_EVENTS, 1, delegate, "onSseStreamEvent");
    }

    private record JobCompletionAwaiterDelegate(JobCompletionAwaiterImpl jobCompletionAwaiter) {

        @SuppressFBWarnings("UPM")
        public void onSseStreamEvent(SseStreamEvent sseStreamEvent) {
            TenantContext.runWithTenantId(
                (String) sseStreamEvent.getMetadata(CURRENT_TENANT_ID),
                () -> jobCompletionAwaiter.onSseStreamEvent(sseStreamEvent));
        }
    }
}
