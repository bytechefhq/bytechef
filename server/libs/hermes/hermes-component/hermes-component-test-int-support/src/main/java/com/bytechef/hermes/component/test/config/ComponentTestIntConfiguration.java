
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

package com.bytechef.hermes.component.test.config;

import com.bytechef.event.listener.EventListener;
import com.bytechef.event.EventPublisher;
import com.bytechef.event.listener.EventListenerChain;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.context.factory.ContextConnectionFactory;
import com.bytechef.hermes.component.context.factory.ContextConnectionFactoryImpl;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import com.bytechef.hermes.component.context.factory.ContextFactory;
import com.bytechef.hermes.component.context.factory.ContextFactoryImpl;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistryImpl;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionServiceImpl;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionServiceImpl;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.configuration.repository.resource.config.ResourceWorkflowRepositoryConfiguration;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.hermes.component.test.JobTestExecutor;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@ComponentScan("com.bytechef.hermes.component")
@EnableAutoConfiguration(
    exclude = {
        DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class
    })
@Import({
    ResourceWorkflowRepositoryConfiguration.class,
})
@SpringBootConfiguration
public class ComponentTestIntConfiguration {

    @MockBean(name = "connectionService")
    private ConnectionService connectionService;

    @MockBean(name = "connectionDefinitionService")
    private ConnectionDefinitionService connectionDefinitionService;

    @MockBean(name = "dataStorageService")
    private DataStorageService dataStorageService;

    @Bean
    ActionDefinitionService actionDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextConnectionFactory contextConnectionFactory,
        ContextFactory contextFactory) {

        return new ActionDefinitionServiceImpl(
            componentDefinitionRegistry, contextConnectionFactory, contextFactory);
    }

    @Bean
    ComponentDefinitionRegistry componentDefinitionRegistry(
        List<ComponentDefinitionFactory> componentDefinitionFactories) {

        return new ComponentDefinitionRegistryImpl(componentDefinitionFactories);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService(ComponentDefinitionRegistry componentDefinitionRegistry) {
        return new ComponentDefinitionServiceImpl(componentDefinitionRegistry);
    }

    @Bean
    ContextConnectionFactory contextConnectionFactory(
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService) {

        return new ContextConnectionFactoryImpl(componentDefinitionService, connectionDefinitionService);
    }

    @Bean
    ContextFactory contextFactory(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        DataStorageService dataStorageService, EventPublisher eventPublisher, FileStorageService fileStorageService) {

        return new ContextFactoryImpl(
            connectionDefinitionService, connectionService, dataStorageService, eventPublisher, fileStorageService);
    }

    @MockBean
    private MessageBroker messageBroker;

    @Bean
    FileStorageService fileStorageService() {
        return new Base64FileStorageService();
    }

    @EnableCaching
    @TestConfiguration
    public static class CacheConfiguration {
    }

    @TestConfiguration
    public static class EncryptionIntTestConfiguration {

        @Bean
        Encryption encryption(EncryptionKey encryptionKey) {
            return new Encryption(encryptionKey);
        }

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }
    }

    @TestConfiguration
    public static class EventConfiguration {

        @Bean
        EventPublisher eventPublisher(List<EventListener> eventListeners) {
            EventListener eventListener = new EventListenerChain(eventListeners);

            return eventListener::onApplicationEvent;
        }
    }

    @TestConfiguration
    public static class WorkflowExecutorConfiguration {

        @Bean
        JobTestExecutor componentWorkflowTestSupport(
            ContextService contextService, EventPublisher eventPublisher, JobService jobService,
            TaskExecutionService taskExecutionService, Map<String, TaskHandler<?>> taskHandlerMap,
            WorkflowService workflowService) {

            return new JobTestExecutor(
                contextService, jobService, eventPublisher, taskExecutionService, taskHandlerMap, workflowService);
        }

        @Bean
        ContextService contextService() {
            return new ContextServiceImpl(new InMemoryContextRepository());
        }

        @Bean
        CounterService counterService() {
            return new CounterServiceImpl(new InMemoryCounterRepository());
        }

        @Bean
        JobService jobService(ObjectMapper objectMapper) {
            return new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository(), objectMapper));
        }

        @Bean
        TaskExecutionService taskExecutionService() {
            return new TaskExecutionServiceImpl(taskExecutionRepository());
        }

        @Bean
        InMemoryTaskExecutionRepository taskExecutionRepository() {
            return new InMemoryTaskExecutionRepository();
        }

        @Bean
        WorkflowService workflowService(List<WorkflowRepository> workflowRepositories) {
            return new WorkflowServiceImpl(
                new ConcurrentMapCacheManager(), Collections.emptyList(), workflowRepositories);
        }
    }
}
