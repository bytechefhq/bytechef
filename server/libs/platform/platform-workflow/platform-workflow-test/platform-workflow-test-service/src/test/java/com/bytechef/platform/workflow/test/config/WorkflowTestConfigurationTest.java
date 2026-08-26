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

package com.bytechef.platform.workflow.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * Guards the hand-assembled dispatcher and completion-handler lists this configuration feeds to its
 * {@code JobSyncExecutor}. Unlike the production coordinator, which autowires every
 * {@code TaskDispatcherResolverFactory} bean, these lists are literals -- so a task dispatcher module can be added to
 * this module's Gradle dependencies, register its own {@code @Bean}s, and still never be reached here. The workflow
 * then falls through to the worker, which reports the dispatcher type as an unknown component.
 *
 * @author Ivica Cardic
 */
class WorkflowTestConfigurationTest {

    private static final String TASK_DISPATCHER_PACKAGE = "com.bytechef.task.dispatcher";

    private final WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

    @Test
    void testEveryTaskCompletionHandlerOnClasspathIsRegistered() {
        Set<String> registeredClassNames = workflowTestConfiguration
            .getTaskCompletionHandlerFactories(
                mock(ContextService.class), mock(CounterService.class), mock(Evaluator.class),
                mock(TaskExecutionService.class), mock(TaskFileStorage.class))
            .stream()
            .map(
                taskCompletionHandlerFactory -> taskCompletionHandlerFactory.createTaskCompletionHandler(
                    mock(TaskCompletionHandler.class), mockTaskDispatcher()))
            .map(taskCompletionHandler -> taskCompletionHandler.getClass()
                .getName())
            .collect(Collectors.toSet());

        assertThat(registeredClassNames).containsAll(findClassNames(TaskCompletionHandler.class));
    }

    @Test
    void testEveryTaskDispatcherOnClasspathIsRegistered() {
        Set<String> registeredClassNames = workflowTestConfiguration
            .getTaskDispatcherResolverFactories(
                mock(ContextService.class), mock(CounterService.class), mock(Evaluator.class),
                mock(ApplicationEventPublisher.class), mock(JobService.class), mock(SubflowResolver.class),
                mock(TaskExecutionService.class), mock(TaskFileStorage.class), mock(WorkflowService.class))
            .stream()
            .map(
                taskDispatcherResolverFactory -> taskDispatcherResolverFactory.createTaskDispatcherResolver(
                    mockTaskDispatcher()))
            .map(taskDispatcherResolver -> taskDispatcherResolver.getClass()
                .getName())
            .collect(Collectors.toSet());

        assertThat(registeredClassNames).containsAll(findClassNames(TaskDispatcherResolver.class));
    }

    private static Set<String> findClassNames(Class<?> type) {
        ClassPathScanningCandidateComponentProvider provider =
            new ClassPathScanningCandidateComponentProvider(false);

        provider.addIncludeFilter(new AssignableTypeFilter(type));

        Set<String> classNames = provider.findCandidateComponents(TASK_DISPATCHER_PACKAGE)
            .stream()
            .map(BeanDefinition::getBeanClassName)
            .collect(Collectors.toSet());

        assertThat(classNames).isNotEmpty();

        return classNames;
    }

    @SuppressWarnings("unchecked")
    private static TaskDispatcher<? super Task> mockTaskDispatcher() {
        return mock(TaskDispatcher.class);
    }
}
