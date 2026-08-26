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
import com.bytechef.automation.configuration.audit.ProjectAuditPublisher;
import com.bytechef.automation.configuration.audit.ProjectDeploymentAuditPublisher;
import com.bytechef.automation.configuration.audit.ProjectWorkflowAuditPublisher;
import com.bytechef.automation.configuration.callback.ProjectCallback;
import com.bytechef.automation.configuration.callback.ProjectWorkflowCallback;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ProjectVisibilityPolicy;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
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
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
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
 * Integration-test configuration for the automation-ai-agent service slice. Brings up the agent service/repository
 * beans plus the narrow slice of
 * {@code automation-configuration}/{@code atlas-configuration}/{@code platform-configuration} beans
 * {@link com.bytechef.automation.ai.agent.facade.AiAgentFacadeImpl} needs to provision, regenerate, and publish an
 * agent's hidden backing project: {@code ProjectService}, {@code ProjectWorkflowService},
 * {@code ProjectDeploymentService}, {@code ProjectDeploymentWorkflowService}, {@code WorkflowService} (JDBC-backed),
 * and — for {@code publishAgent}'s replicated {@code ProjectFacadeImpl.publishProject} loop —
 * {@code WorkflowTestConfigurationService} and {@code WorkflowNodeTestOutputService}. {@code TriggerDefinitionService}
 * is mocked rather than wired for real: its only callers are
 * {@link com.bytechef.automation.ai.agent.facade.AiAgentFacadeImpl#getAgentDeployments} and
 * {@code getWorkspaceChatAgents}, whose tests stub the handful of trigger definitions they need per test, and a real
 * bean would need the full component-definition registry this narrow slice deliberately avoids pulling in (see class
 * javadoc below).
 *
 * <p>
 * Deliberately does NOT component-scan {@code com.bytechef.automation.configuration}: that would also pick up
 * {@code ProjectFacadeImpl} and its workflow-execution/component-connection dependencies, which this slice doesn't
 * exercise. Each needed {@code *ServiceImpl}/{@code *AuditPublisher}/callback is imported directly instead (same
 * precedent as {@link ProjectCallback} in Task 6). The {@code category}, {@code tag}, {@code api_key}, and
 * {@code connection} tables still need to physically exist because {@code Project}'s Liquibase changeset FKs into them;
 * the corresponding service modules are testImplementation deps purely so their changelogs are on the classpath, not
 * for their Spring beans.
 * </p>
 *
 * <p>
 * {@code WorkflowService} is wired the same way {@code WorkflowServiceIntTest} (atlas-configuration-service) wires it:
 * a manual {@code @Bean} over {@code WorkflowServiceImpl}, fed by whatever {@code WorkflowCrudRepository}/
 * {@code WorkflowRepository} beans are on the classpath. {@code JdbcWorkflowRepository} only registers itself when
 * {@code bytechef.workflow.repository.jdbc.enabled=true} —
 * {@link com.bytechef.automation.ai.agent.facade.AiAgentFacadeIntTest} sets that property;
 * {@link com.bytechef.automation.ai.agent.service.AiAgentServiceIntTest} does not and simply never exercises the
 * (harmlessly empty) {@code WorkflowCrudRepository} list.
 * </p>
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.automation.ai.agent")
@EnableAutoConfiguration
@EnableCaching
@Import({
    LiquibaseConfiguration.class, JacksonConfiguration.class, ProjectServiceImpl.class,
    ProjectWorkflowServiceImpl.class, ProjectDeploymentServiceImpl.class, ProjectDeploymentWorkflowServiceImpl.class,
    ProjectAuditPublisher.class, ProjectWorkflowAuditPublisher.class, ProjectDeploymentAuditPublisher.class,
    ProjectWorkflowCallback.class, WorkflowTestConfigurationServiceImpl.class, WorkflowNodeTestOutputServiceImpl.class
})
@Configuration
public class AutomationAiAgentIntTestConfiguration {

    @Bean
    ProjectCallback projectCallback() {
        return new ProjectCallback();
    }

    /**
     * {@code ProjectServiceImpl.updateVisibility} validates the requested rung against this registry. The production
     * bean is assembled in platform-connection-api's {@code ResourceVisibilityConfiguration}, which this slice does not
     * scan, so it is declared here over the real {@link ProjectVisibilityPolicy} rather than mocked — a mock would let
     * an unsupported rung through and make the slice disagree with production.
     */
    @Bean
    ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry() {
        return new ResourceVisibilityPolicyRegistry(List.of(new ProjectVisibilityPolicy()));
    }

    /**
     * {@code AiAgentFacadeImpl}'s agent and agent-deployment listings filter through this. Declared here over the real
     * {@link ProjectVisibilityFilter} rather than mocked, for the same reason as the registry above, and with no
     * resolver behind it: this slice carries neither edition's {@code ResourceVisibilityResolver}, and the filter's own
     * no-resolver branch hides every project. An empty {@code ObjectProvider} would therefore empty both listings and
     * make every test of them fail for a reason that has nothing to do with what it asserts, so the resolver supplied
     * here admits everything — visibility itself is covered by {@code AiAgentFacadeVisibilityFilterTest} and
     * {@code PermissionServiceAgentVisibilityTest}, which stub the resolver per case.
     */
    @Bean
    ProjectVisibilityFilter projectVisibilityFilter() {
        ResourceVisibilityResolver resourceVisibilityResolver =
            (resourceType, workspaceId, candidates) -> candidates.stream()
                .map(VisibilityRecord::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new ProjectVisibilityFilter(new SingletonObjectProvider<>(resourceVisibilityResolver));
    }

    /**
     * The narrowest possible {@link ObjectProvider}: {@link ProjectVisibilityFilter} calls nothing on it but
     * {@code getIfAvailable}, and Spring offers no ready-made single-value implementation outside a bean factory.
     */
    private record SingletonObjectProvider<T>(T instance) implements ObjectProvider<T> {

        @Override
        public T getObject() {
            return instance;
        }

        @Override
        public T getObject(Object... args) {
            return instance;
        }

        @Override
        public T getIfAvailable() {
            return instance;
        }

        @Override
        public T getIfUnique() {
            return instance;
        }
    }

    @Bean
    TriggerDefinitionService triggerDefinitionService() {
        return Mockito.mock(TriggerDefinitionService.class);
    }

    /**
     * {@code AgentChannelResolver} (component-scanned with the rest of this module) resolves every stored channel key
     * against the component registry. This slice deliberately does not pull that registry in, so the service is stubbed
     * with the handful of components these tests reach — see {@link TestComponentDefinitions}, which builds real SDK
     * definitions rather than mocking the DTOs, so the flattening the resolver depends on is genuinely exercised.
     */
    @Bean
    ComponentDefinitionService componentDefinitionService() {
        return TestComponentDefinitions.componentDefinitionService();
    }

    // getAgentDeployments reads a deployment's last execution through these, exactly as ProjectDeploymentFacadeImpl
    // does. Mocked for the same reason triggerDefinitionService() is: no test in this slice runs a job, and the real
    // beans would pull the execution stack into a slice that deliberately avoids it.
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

    // CallableAiAgentDataSourceImpl's workspace-accessibility check needs a UserService/WorkspaceFacade pair (same
    // beans SubflowDataSourceImpl depends on in automation-configuration-service) -- mocked rather than wired for
    // real, same reasoning as triggerDefinitionService() above: no test in this slice needs a real principal/
    // workspace-membership lookup, and wiring the real beans would pull in the security/workspace-membership stack
    // this narrow slice deliberately avoids (see class javadoc). CallableAiAgentDataSourceIntTest stubs these per test.

    @Bean
    UserService userService() {
        return Mockito.mock(UserService.class);
    }

    // Same reasoning as the mocks above: no test in this slice asserts on agent tags, and Mockito's default empty
    // list for getTags keeps toAgentDTO's read path working. Wire a real TagServiceImpl here if tag behaviour ever
    // needs covering.
    @Bean
    TagService tagService() {
        return Mockito.mock(TagService.class);
    }

    @Bean
    WorkspaceFacade workspaceFacade() {
        return Mockito.mock(WorkspaceFacade.class);
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
