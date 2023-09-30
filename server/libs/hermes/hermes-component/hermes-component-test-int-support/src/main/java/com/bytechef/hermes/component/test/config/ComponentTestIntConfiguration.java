
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

import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacadeImpl;
import com.bytechef.atlas.worker.task.factory.TaskHandlerMapFactory;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.data.storage.service.DataStorageService;
import com.bytechef.hermes.connection.service.RemoteConnectionService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.configuration.repository.resource.config.ResourceWorkflowRepositoryConfiguration;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.RemoteCounterService;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
import com.bytechef.atlas.execution.service.RemoteJobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.configuration.service.RemoteWorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.hermes.component.test.JobTestExecutor;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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
    ResourceWorkflowRepositoryConfiguration.class
})
@Configuration
public class ComponentTestIntConfiguration {

    @MockBean(name = "connectionService")
    private RemoteConnectionService connectionService;

    @MockBean(name = "dataStorageService")
    private DataStorageService dataStorageService;

    @MockBean
    private MessageBroker messageBroker;

    @Bean
    FileStorageService fileStorageService() {
        return new Base64FileStorageService();
    }

    @TestConfiguration
    public static class EncryptionIntTestConfiguration {

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }
    }

    @TestConfiguration
    public static class JacksonConfiguration {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
        }

        @Bean
        XmlMapper xmlMapper() {
            return XmlMapper.xmlBuilder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
        }
    }

    @TestConfiguration
    public static class WorkflowConfiguration {

        @Bean
        RemoteWorkflowService workflowService(List<WorkflowRepository> workflowRepositories) {
            return new WorkflowServiceImpl(
                new ConcurrentMapCacheManager(), Collections.emptyList(), workflowRepositories);
        }
    }

    @TestConfiguration
    public static class WorkflowExecutionConfiguration {

        @Bean
        JobTestExecutor componentWorkflowTestSupport(
            RemoteContextService contextService, RemoteJobService jobService, ObjectMapper objectMapper,
            RemoteTaskExecutionService taskExecutionService,
            Map<String, TaskHandler<?>> taskHandlerMap, TaskHandlerMapFactory taskHandlerMapFactory,
            RemoteWorkflowService workflowService) {

            return new JobTestExecutor(
                contextService, jobService, objectMapper, taskExecutionService,
                MapUtils.concat(taskHandlerMap, taskHandlerMapFactory.getTaskHandlerMap()), workflowService);
        }

        @Bean
        RemoteContextService contextService() {
            return new ContextServiceImpl(new InMemoryContextRepository());
        }

        @Bean
        RemoteCounterService counterService() {
            return new CounterServiceImpl(new InMemoryCounterRepository());
        }

        @Bean
        RemoteJobService jobService(ObjectMapper objectMapper) {
            return new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository(), objectMapper));
        }

        @Bean
        RemoteTaskExecutionService taskExecutionService() {
            return new TaskExecutionServiceImpl(taskExecutionRepository());
        }

        @Bean
        InMemoryTaskExecutionRepository taskExecutionRepository() {
            return new InMemoryTaskExecutionRepository();
        }
    }

    @TestConfiguration
    public static class WorkflowFileStorageConfiguration {

        @Bean
        WorkflowFileStorageFacade workflowFileStorageFacade(ObjectMapper objectMapper) {
            return new WorkflowFileStorageFacadeImpl(new Base64FileStorageService(), objectMapper);
        }
    }
}
