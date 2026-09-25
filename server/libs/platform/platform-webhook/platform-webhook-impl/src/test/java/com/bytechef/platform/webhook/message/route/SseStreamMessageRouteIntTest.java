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

package com.bytechef.platform.webhook.message.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.redis.config.RedisMessageBrokerConfiguration;
import com.bytechef.message.broker.redis.env.RedisMessageBrokerListenerRegistrarConfiguration;
import com.bytechef.platform.coordinator.event.listener.SseStreamApplicationEventListener;
import com.bytechef.platform.webhook.executor.JobCompletionAwaiterImpl;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry;
import com.bytechef.platform.webhook.executor.config.JobCompletionAwaiterConfiguration;
import com.bytechef.platform.webhook.executor.config.SseStreamMessageBrokerConfigurerConfiguration;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class SseStreamMessageRouteIntTest {

    private static final long FUTURE_WAIT_SECONDS = 30;
    private static final int REDIS_PORT = 6379;

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(
        DockerImageName.parse("redis:7-alpine")).withExposedPorts(REDIS_PORT);

    private final List<AnnotationConfigApplicationContext> applicationContexts = new ArrayList<>();

    @AfterEach
    void afterEach() {
        for (AnnotationConfigApplicationContext applicationContext : applicationContexts) {
            applicationContext.close();
        }
    }

    @Test
    void testSseStreamEventsRouteIsBroadcastToEveryInstance() throws Exception {
        AnnotationConfigApplicationContext webhookInstanceContext = startInstance(
            applicationContext -> {
                applicationContext.register(JobCompletionAwaiterConfiguration.class);
                applicationContext.registerBean(
                    JobService.class,
                    () -> new JobServiceImpl(
                        new InMemoryJobRepository(
                            new InMemoryTaskExecutionRepository(), applicationContext.getBean(ObjectMapper.class))));
            });

        AnnotationConfigApplicationContext coordinatorInstanceContext = startInstance(
            applicationContext -> {
                applicationContext.register(SseStreamMessageBrokerConfigurerConfiguration.class);
                applicationContext.registerBean(SseStreamBridgeRegistry.class);
            });

        JobCompletionAwaiterImpl jobCompletionAwaiter = webhookInstanceContext.getBean(JobCompletionAwaiterImpl.class);
        JobService jobService = webhookInstanceContext.getBean(JobService.class);
        SseStreamBridgeRegistry sseStreamBridgeRegistry = coordinatorInstanceContext.getBean(
            SseStreamBridgeRegistry.class);

        Job job = new Job();

        job.setStatus(Job.Status.STARTED);

        job = jobService.update(job);

        long jobId = Objects.requireNonNull(job.getId());

        SseStreamBridgeRegistry.Registration registration = sseStreamBridgeRegistry.register(jobId, payload -> {});

        CompletableFuture<Job> future = jobCompletionAwaiter.await(jobId, Duration.ofSeconds(60));

        job.setStatus(Job.Status.COMPLETED);

        jobService.update(job);

        SseStreamApplicationEventListener sseStreamApplicationEventListener = new SseStreamApplicationEventListener(
            coordinatorInstanceContext.getBean(MessageBroker.class), mock(TaskExecutionService.class));

        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(jobId, Job.Status.COMPLETED));

        Job completedJob = future.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS);

        assertThat(completedJob).isEqualTo(job);
        assertThat(completedJob.getStatus()).isEqualTo(Job.Status.COMPLETED);

        CompletableFuture<Void> streamCompletion = registration.completion()
            .toCompletableFuture();

        streamCompletion.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS);
    }

    private AnnotationConfigApplicationContext startInstance(
        Consumer<AnnotationConfigApplicationContext> instanceCustomizer) {

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

        applicationContexts.add(applicationContext);

        MutablePropertySources propertySources = applicationContext.getEnvironment()
            .getPropertySources();

        propertySources.addFirst(
            new MapPropertySource("sseStreamMessageRouteIntTest", Map.of("bytechef.message-broker.provider", "redis")));

        applicationContext.registerBean(
            RedisConnectionFactory.class,
            () -> new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redisContainer.getHost(), redisContainer.getMappedPort(REDIS_PORT))));
        applicationContext.registerBean(
            ObjectMapper.class, () -> JsonMapper.builder()
                .build());
        applicationContext.registerBean(
            "applicationTaskExecutor", TaskExecutor.class, () -> new SimpleAsyncTaskExecutor());
        applicationContext.register(
            RedisMessageBrokerConfiguration.class, RedisMessageBrokerListenerRegistrarConfiguration.class);

        instanceCustomizer.accept(applicationContext);

        applicationContext.refresh();

        return applicationContext;
    }
}
