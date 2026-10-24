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

package com.bytechef.automation.datasync.config;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.callback.ProjectCallback;
import com.bytechef.automation.configuration.callback.ProjectWorkflowCallback;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.EnvironmentServiceImpl;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputServiceImpl;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationServiceImpl;
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
 * Integration-test configuration for the automation-data-sync service slice. Brings up the data-sync service/repository
 * beans plus the narrow slice of {@code automation-configuration}/{@code atlas-configuration}/
 * {@code platform-configuration} beans {@code DataSyncFacadeImpl} needs to create, regenerate, publish and delete a
 * Data Sync inside an ordinary project: {@code ProjectService}, {@code ProjectWorkflowService},
 * {@code ProjectDeploymentService}, {@code ProjectDeploymentWorkflowService}, {@code WorkflowService} (JDBC-backed),
 * {@code WorkflowTestConfigurationService} (the draft's test connections, removed again on delete) and
 * {@code WorkflowNodeTestOutputService}. {@code ProjectFacadeImpl} is not wired, so {@code DataSyncFacadeIntTest}
 * replays its publish body by hand. {@code TriggerDefinitionService} and {@code ComponentDefinitionService} are mocked
 * rather than wired for real: no test in this slice runs the component-definition registry, and a real bean would need
 * the full registry this narrow slice deliberately avoids pulling in.
 *
 * <p>
 * Deliberately does NOT component-scan {@code com.bytechef.automation.configuration}: that would also pick up
 * {@code ProjectFacadeImpl} and its workflow-execution/component-connection dependencies, which this slice doesn't
 * exercise. Each needed {@code *ServiceImpl}/callback is imported directly instead (same precedent as
 * {@link ProjectCallback} in automation-ai-agent's own IntTest configuration).
 * </p>
 *
 * <p>
 * Unlike {@code AutomationAiAgentIntTestConfiguration}, this slice does not declare {@code UserService}/
 * {@code WorkspaceFacade} mocks: those exist only for the agent module's {@code CallableAiAgentDataSourceImpl}
 * workspace-accessibility check, and Data Sync has no analogue of that data source.
 * </p>
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.automation.datasync")
@EnableAutoConfiguration
@EnableCaching
@Import({
    LiquibaseConfiguration.class, JacksonConfiguration.class, ProjectServiceImpl.class,
    ProjectWorkflowServiceImpl.class, ProjectDeploymentServiceImpl.class, ProjectDeploymentWorkflowServiceImpl.class,
    ProjectWorkflowCallback.class, WorkflowTestConfigurationServiceImpl.class, WorkflowNodeTestOutputServiceImpl.class
})
@Configuration
public class AutomationDataSyncIntTestConfiguration {

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
        return Mockito.mock(ComponentDefinitionService.class);
    }

    @Bean
    ProjectDeploymentFacade projectDeploymentFacade() {
        return Mockito.mock(ProjectDeploymentFacade.class);
    }

    // getAgentDeployments-equivalent listings would read a deployment's last execution through these, exactly as
    // ProjectDeploymentFacadeImpl does. Mocked for the same reason triggerDefinitionService() is: no test in this
    // slice runs a job, and the real beans would pull the execution stack into a slice that deliberately avoids it.
    @Bean
    PrincipalJobService principalJobService() {
        return Mockito.mock(PrincipalJobService.class);
    }

    @Bean
    JobService jobService() {
        return Mockito.mock(JobService.class);
    }

    /**
     * Declared as a plain {@code @Bean} rather than imported: {@link EnvironmentServiceImpl} carries
     * {@code @ConditionalOnCEVersion}, which would need this slice to also set {@code bytechef.edition} just to obtain
     * what is effectively a pure {@code Environment.values()} lookup.
     */
    @Bean
    EnvironmentService environmentService() {
        return new EnvironmentServiceImpl();
    }

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    // Collaborator of the imported WorkflowNodeTestOutputServiceImpl, which evicts the workflow-scoped output caches
    // when node test outputs are deleted. Mocked for the same reason as the beans above: the real
    // WorkflowCacheManagerImpl lives in a package this slice does not scan, and eviction is not what these tests
    // assert on.
    @Bean
    WorkflowCacheManager workflowCacheManager() {
        return Mockito.mock(WorkflowCacheManager.class);
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
    public static class AutomationDataSyncIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public AutomationDataSyncIntTestJdbcConfiguration(ObjectMapper objectMapper) {
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
