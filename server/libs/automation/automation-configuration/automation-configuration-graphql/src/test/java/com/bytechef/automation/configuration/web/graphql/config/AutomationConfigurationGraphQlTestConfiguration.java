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

package com.bytechef.automation.configuration.web.graphql.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import java.util.stream.Collectors;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * @author Ivica Cardic
 */
@Configuration
public class AutomationConfigurationGraphQlTestConfiguration {

    @Bean
    @Primary
    public CategoryService categoryService() {
        return Mockito.mock(CategoryService.class);
    }

    /**
     * Mirrors the CE implementation's permissive pass-through: these controller tests pin wiring and schema mapping,
     * not authorization, which {@code PermissionServiceResourceTest} covers.
     *
     * <p>
     * Only {@code hasResourceScope} is stubbed, because it is the only method this module reaches: the module's
     * {@code @PreAuthorize}s are all {@code hasPermission(...)} expressions, and {@code AutomationPermissionEvaluator}
     * routes every one of them here. The facades that call {@code PermissionService} directly are Mockito mocks in this
     * context, so their calls never run.
     */
    @Bean
    @Primary
    public PermissionService permissionService() {
        PermissionService permissionService = Mockito.mock(PermissionService.class);

        Mockito
            .when(
                permissionService.hasResourceScope(
                    ArgumentMatchers.any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(true);

        return permissionService;
    }

    @Bean
    @Primary
    public ProjectFacade projectFacade() {
        return Mockito.mock(ProjectFacade.class);
    }

    /**
     * The real filter over a resolver that hides nothing, so the controller tests see every project they stub. The
     * filtering branch itself is pinned by {@code ProjectVisibilityFilterTest} and the facade visibility tests.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public ProjectVisibilityFilter projectVisibilityFilter() {
        ResourceVisibilityResolver resourceVisibilityResolver =
            (resourceType, workspaceId, candidates) -> candidates.stream()
                .map(VisibilityRecord::id)
                .collect(Collectors.toSet());

        ObjectProvider<ResourceVisibilityResolver> objectProvider = Mockito.mock(ObjectProvider.class);

        Mockito.when(objectProvider.getIfAvailable())
            .thenReturn(resourceVisibilityResolver);

        return new ProjectVisibilityFilter(objectProvider);
    }

    @Bean
    @Primary
    public ProjectService projectService() {
        return Mockito.mock(ProjectService.class);
    }

    @Bean
    @Primary
    public ProjectWorkflowFacade projectWorkflowFacade() {
        return Mockito.mock(ProjectWorkflowFacade.class);
    }

    @Bean
    @Primary
    public ProjectWorkflowService projectWorkflowService() {
        return Mockito.mock(ProjectWorkflowService.class);
    }

    @Bean
    @Primary
    public TagService tagService() {
        return Mockito.mock(TagService.class);
    }

    @Bean
    @Primary
    public UserService userService() {
        return Mockito.mock(UserService.class);
    }

    @Bean
    @Primary
    public WorkflowService workflowService() {
        return Mockito.mock(WorkflowService.class);
    }

    @Bean
    @Primary
    public WorkflowFacade workflowFacade() {
        return Mockito.mock(WorkflowFacade.class);
    }

    @Bean
    RuntimeWiringConfigurer mapScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.mapScalar());
    }

    @Bean
    @Primary
    public WorkspaceConnectionFacade workspaceConnectionFacade() {
        return Mockito.mock(WorkspaceConnectionFacade.class);
    }

    @Bean
    public RuntimeWiringConfigurer longScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
    }

    @Bean
    @Primary
    public WorkspaceUserService workspaceUserService() {
        return Mockito.mock(WorkspaceUserService.class);
    }
}
