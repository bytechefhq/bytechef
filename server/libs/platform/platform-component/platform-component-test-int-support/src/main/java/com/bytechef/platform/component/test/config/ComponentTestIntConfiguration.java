/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.test.config;

import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.repository.resource.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerFactory;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.component.test.ComponentJobTestExecutor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.bytechef.platform.file.storage.FilesFileStorageImpl;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Ivica Cardic
 */
@ComponentScan("com.bytechef.platform.component")
@EnableAutoConfiguration(
    exclude = {
        DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class
    })
@EnableConfigurationProperties(ApplicationProperties.class)
@Configuration
public class ComponentTestIntConfiguration {

    private final JsonComponentModule jsonComponentModule;

    @SuppressFBWarnings("EI")
    public ComponentTestIntConfiguration(JsonComponentModule jsonComponentModule) {
        this.jsonComponentModule = jsonComponentModule;
    }

    @Bean
    ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Component component = new ApplicationProperties.Component();

        component.setRegistry(new ApplicationProperties.Component.Registry());

        applicationProperties.setComponent(component);

        return applicationProperties;
    }

    @Bean
    ClassPathResourceWorkflowRepository classPathResourceWorkflowRepository(
        ResourcePatternResolver resourcePatternResolver) {

        return new ClassPathResourceWorkflowRepository("workflows/**/*.{json|yml|yaml}", resourcePatternResolver);
    }

    @Bean
    ComponentJobTestExecutor componentWorkflowTestSupport(
        ContextService contextService, JobService jobService,
        TaskExecutionService taskExecutionService, Map<String, TaskHandler<?>> taskHandlerMap,
        TaskHandlerFactory taskHandlerFactory, WorkflowService workflowService) {

        return new ComponentJobTestExecutor(
            contextService, jobService, taskExecutionService,
            MapUtils.concat(taskHandlerMap, taskHandlerFactory.getTaskHandlerMap()), workflowService);
    }

    @Bean(name = "connectionService")
    ConnectionService connectionService() {
        return Mockito.mock(ConnectionService.class);
    }

    @Bean(name = "dataStorageService")
    DataStorage dataStorage() {
        return Mockito.mock(DataStorage.class);
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
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    FilesFileStorage filesFileStorage() {
        return new FilesFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    MessageBroker messageBroker() {
        return Mockito.mock(MessageBroker.class);
    }

    @Bean
    JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry() {
        return new JobPrincipalAccessorRegistry(List.of());
    }

    @Bean
    JobService jobService(ObjectMapper objectMapper) {
        return new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository(), objectMapper));
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .registerModule(jsonComponentModule);
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
    XmlMapper xmlMapper() {
        return XmlMapper.xmlBuilder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();
    }

    @Bean
    TaskFileStorage workflowFileStorageFacade() {
        return new TaskFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    WorkflowService workflowService(List<WorkflowRepository> workflowRepositories) {
        return new WorkflowServiceImpl(
            new ConcurrentMapCacheManager(), Collections.emptyList(), workflowRepositories);
    }
}
