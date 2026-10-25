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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.broker.redis.listener.RedisListenerEndpointRegistrar;
import com.bytechef.message.broker.redis.serializer.RedisMessageDeserializer;
import com.bytechef.message.route.MessageRoute;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.executor.JobCompletionAwaiterImpl;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.MethodInvoker;

/**
 * @author Ivica Cardic
 */
class SseStreamMessageBrokerConfigurerConfigurationTest {

    private static final long JOB_ID = 9L;

    @Test
    void testAwaiterCompletesWhileTheSseBridgeIsRegisteredOnTheSameRoute() throws Exception {
        JobService jobService = mock(JobService.class);
        Job completedJob = createJob(Job.Status.COMPLETED);

        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(createJob(Job.Status.STARTED)));
        when(jobService.getJob(JOB_ID)).thenReturn(completedJob);

        JobCompletionAwaiterImpl jobCompletionAwaiter = new JobCompletionAwaiterImpl(jobService);
        SseStreamBridgeRegistry sseStreamBridgeRegistry = mock(SseStreamBridgeRegistry.class);

        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);

        doReturn(mock(StreamOperations.class)).when(stringRedisTemplate)
            .opsForStream();

        RedisListenerEndpointRegistrar redisListenerEndpointRegistrar = new RedisListenerEndpointRegistrar(
            mock(RedisMessageDeserializer.class), stringRedisTemplate, mock(TaskExecutor.class));
        ControlRegistrations controlRegistrations = new ControlRegistrations();

        List<MessageBrokerConfigurer<?>> messageBrokerConfigurers = List.of(
            new SseStreamMessageBrokerConfigurerConfiguration().sseStreamMessageBrokerConfigurer(
                sseStreamBridgeRegistry),
            new JobCompletionAwaiterConfiguration().jobCompletionAwaiterMessageBrokerConfigurer(
                jobCompletionAwaiter));

        for (MessageBrokerConfigurer<?> messageBrokerConfigurer : messageBrokerConfigurers) {
            configure(messageBrokerConfigurer, redisListenerEndpointRegistrar, controlRegistrations);
        }

        CompletableFuture<Job> future = jobCompletionAwaiter.await(JOB_ID, Duration.ofSeconds(5));

        SseStreamEvent sseStreamEvent = new SseStreamEvent(
            JOB_ID, SseStreamEvent.EVENT_TYPE_JOB_STATUS, Job.Status.COMPLETED.name());

        sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, TenantContext.DEFAULT_TENANT_ID);

        controlRegistrations.deliver(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);

        assertThat(future.get(2, TimeUnit.SECONDS)).isEqualTo(completedJob);

        verify(sseStreamBridgeRegistry).onSseStreamEvent(sseStreamEvent);
    }

    @SuppressWarnings("unchecked")
    private static void configure(
        MessageBrokerConfigurer<?> messageBrokerConfigurer,
        RedisListenerEndpointRegistrar redisListenerEndpointRegistrar, ControlRegistrations controlRegistrations) {

        MessageBrokerConfigurer<RedisListenerEndpointRegistrar> redisMessageBrokerConfigurer =
            (MessageBrokerConfigurer<RedisListenerEndpointRegistrar>) messageBrokerConfigurer;

        redisMessageBrokerConfigurer.configure(redisListenerEndpointRegistrar,
            new ForwardingListenerRegistrar(controlRegistrations));
    }

    private static Job createJob(Job.Status status) {
        Job job = new Job();

        job.setId(JOB_ID);
        job.setStatus(status);

        return job;
    }

    private record ControlRegistration(MessageRoute messageRoute, Object delegate, String methodName) {
    }

    private static final class ControlRegistrations {

        private final List<ControlRegistration> controlRegistrations = new ArrayList<>();

        private void add(MessageRoute messageRoute, Object delegate, String methodName) {
            controlRegistrations.add(new ControlRegistration(messageRoute, delegate, methodName));
        }

        private void deliver(MessageRoute messageRoute, Object message) throws Exception {
            for (ControlRegistration controlRegistration : controlRegistrations) {
                if (controlRegistration.messageRoute()
                    .getName()
                    .equals(messageRoute.getName())) {

                    MethodInvoker methodInvoker = new MethodInvoker();

                    methodInvoker.setTargetObject(controlRegistration.delegate());
                    methodInvoker.setTargetMethod(controlRegistration.methodName());
                    methodInvoker.setArguments(message);

                    methodInvoker.prepare();

                    methodInvoker.invoke();
                }
            }
        }
    }

    private static final class ForwardingListenerRegistrar
        implements MessageBrokerListenerRegistrar<RedisListenerEndpointRegistrar> {

        private final ControlRegistrations controlRegistrations;

        private ForwardingListenerRegistrar(ControlRegistrations controlRegistrations) {
            this.controlRegistrations = controlRegistrations;
        }

        @Override
        public void registerListenerEndpoint(
            RedisListenerEndpointRegistrar listenerEndpointRegistrar, MessageRoute messageRoute, int concurrency,
            Object delegate, String methodName) {

            controlRegistrations.add(messageRoute, delegate, methodName);
        }

        @Override
        public void stopListenerEndpoints() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startListenerEndpoints() {
            throw new UnsupportedOperationException();
        }
    }
}
