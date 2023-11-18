/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.worker.config;

import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.executor.TaskWorkerExecutor;
import com.bytechef.atlas.worker.task.factory.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "worker.enabled", matchIfMissing = true)
@EnableConfigurationProperties(TaskWorkerProperties.class)
public class TaskWorkerConfiguration {

    private final List<TaskDispatcherAdapterFactory> taskDispatcherAdapterTaskHandlerFactories;

    @SuppressFBWarnings("EI")
    public TaskWorkerConfiguration(
        @Autowired(required = false) List<TaskDispatcherAdapterFactory> taskDispatcherAdapterTaskHandlerFactories) {

        this.taskDispatcherAdapterTaskHandlerFactories = taskDispatcherAdapterTaskHandlerFactories == null
            ? Collections.emptyList() : taskDispatcherAdapterTaskHandlerFactories;
    }

    @Bean
    TaskWorkerExecutor taskWorkerExecutor() {
        return new TaskWorkerExecutor();
    }

    @Bean
    TaskHandlerResolver defaultTaskHandlerResolver(TaskHandlerRegistry taskHandlerRegistry) {
        return new DefaultTaskHandlerResolver(taskHandlerRegistry);
    }

    @Bean
    TaskHandlerResolver taskDispatcherAdapterTaskHandlerResolver(TaskHandlerResolver taskHandlerResolver) {
        return new TaskDispatcherAdapterTaskHandlerResolver(
            taskDispatcherAdapterTaskHandlerFactories, taskHandlerResolver);
    }

    @Bean
    @Primary
    TaskHandlerResolver taskHandlerResolver(TaskHandlerRegistry taskHandlerRegistry) {
        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                taskDispatcherAdapterTaskHandlerResolver(taskHandlerResolverChain),
                defaultTaskHandlerResolver(taskHandlerRegistry)));

        return taskHandlerResolverChain;
    }

    @Bean
    TaskWorker taskWorker(
        ApplicationEventPublisher eventPublisher, TaskWorkerExecutor taskWorkerExecutor,
        TaskHandlerResolver taskHandlerResolver, TaskFileStorage taskFileStorage) {

        return new TaskWorker(
            eventPublisher, new ExecutorServiceWrapper(taskWorkerExecutor), taskHandlerResolver,
            taskFileStorage);
    }

    private record ExecutorServiceWrapper(TaskWorkerExecutor taskWorkerExecutor) implements ExecutorService {

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isShutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isTerminated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return null;
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void execute(Runnable command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<?> submit(Runnable task) {
            return taskWorkerExecutor.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }
}
