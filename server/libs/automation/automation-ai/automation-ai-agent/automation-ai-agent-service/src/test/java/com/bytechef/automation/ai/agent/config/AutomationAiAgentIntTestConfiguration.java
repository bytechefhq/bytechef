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

package com.bytechef.automation.ai.agent.config;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.callback.ProjectCallback;
import com.bytechef.automation.configuration.callback.ProjectWorkflowCallback;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.EnvironmentServiceImpl;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationServiceImpl;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.automation.ai.agent")
@EnableAutoConfiguration
@EnableCaching
@Import({
    LiquibaseConfiguration.class, JacksonConfiguration.class, ProjectServiceImpl.class,
    ProjectWorkflowServiceImpl.class, ProjectDeploymentServiceImpl.class, ProjectDeploymentWorkflowServiceImpl.class,
    ProjectWorkflowCallback.class, WorkflowTestConfigurationServiceImpl.class
})
@Configuration
public class AutomationAiAgentIntTestConfiguration {

    public static final List<Long> WORKSPACE_CONNECTION_IDS = List.of(42L, 55L, 77L, 99L, 100L);

    public static final List<Long> WORKSPACE_KNOWLEDGE_BASE_IDS = List.of(300L);

    @Bean
    ProjectCallback projectCallback() {
        return new ProjectCallback();
    }

    @Bean
    TriggerDefinitionService triggerDefinitionService() {
        return Mockito.mock(TriggerDefinitionService.class);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService() {
        return TestComponentDefinitions.componentDefinitionService();
    }

    @Bean
    PrincipalJobService principalJobService() {
        return Mockito.mock(PrincipalJobService.class);
    }

    @Bean
    JobService jobService() {
        return Mockito.mock(JobService.class);
    }

    @Bean
    EnvironmentService environmentService() {
        return new EnvironmentServiceImpl();
    }

    @Bean
    WorkspaceConnectionService workspaceConnectionService() {
        WorkspaceConnectionService workspaceConnectionService = Mockito.mock(WorkspaceConnectionService.class);

        Mockito.when(workspaceConnectionService.getWorkspaceConnections(Mockito.anyLong()))
            .thenAnswer(invocation -> WORKSPACE_CONNECTION_IDS.stream()
                .map(connectionId -> new WorkspaceConnection(connectionId, invocation.getArgument(0)))
                .toList());

        return workspaceConnectionService;
    }

    @Bean
    WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService() {
        WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService = Mockito.mock(
            WorkspaceKnowledgeBaseService.class);

        Mockito.when(workspaceKnowledgeBaseService.getWorkspaceKnowledgeBases(Mockito.anyLong()))
            .thenAnswer(invocation -> WORKSPACE_KNOWLEDGE_BASE_IDS.stream()
                .map(knowledgeBaseId -> new WorkspaceKnowledgeBase(knowledgeBaseId, invocation.getArgument(0)))
                .toList());

        return workspaceKnowledgeBaseService;
    }

    @Bean
    UserService userService() {
        return Mockito.mock(UserService.class);
    }

    @Bean
    WorkspaceFacade workspaceFacade() {
        return Mockito.mock(WorkspaceFacade.class);
    }

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .build();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AutomationAgentIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public AutomationAgentIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return Arrays.asList(
                new MapWrapperToStringConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
