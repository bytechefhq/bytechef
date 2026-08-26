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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.security.ProjectWorkflowOwnershipResolver;
import com.bytechef.automation.configuration.security.ProjectWorkflowVisibilityProvider;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.DefaultResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Closes the two read paths on {@link ProjectWorkflowFacadeImpl} that carried no visibility check at all:
 * {@code getProjectWorkflow(long)} — the workflow editor's primary read, which returns the whole definition — and the
 * no-argument {@code getProjectWorkflows()}, which backs the "All projects" sidebar and returns every project workflow
 * row in the tenant.
 *
 * <p>
 * Both are management surfaces under {@code /api/automation/internal}; neither is on a runtime path, so a withheld
 * project's deployments keep serving traffic.
 *
 * <p>
 * Every case below is written so that VISIBILITY is the only thing that can produce the outcome. The by-id tests run
 * the real {@code @PreAuthorize} expression through the real evaluator, the real CE {@code PermissionServiceImpl} and
 * the real {@code ProjectWorkflow} provider/resolver pair, and differ from one another in exactly one field — the
 * owning project's {@code visibility} column. The list test hands both rows a resolvable workflow, so the facade's
 * {@code Objects::nonNull} branch cannot be what drops one of them.
 *
 * @author Ivica Cardic
 */
class ProjectWorkflowFacadeVisibilityTest {

    private static final String OTHER_USER = "ivica";
    private static final long PRIVATE_PROJECT_ID = 2L;
    private static final long PROJECT_WORKFLOW_ID = 8L;
    private static final String USER = "ana";
    private static final long WORKSPACE_ID = 1L;
    private static final long WORKSPACE_PROJECT_ID = 1L;

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowRepository projectWorkflowRepository = mock(ProjectWorkflowRepository.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowFacade workflowFacade = mock(WorkflowFacade.class);

    @BeforeEach
    void setUp() {
        authenticate(USER);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Red-verification: deleting the {@code @PreAuthorize} line makes {@code guardExpression()} fail on the
     * {@code isNotNull} assertion, so this test cannot pass an unguarded method.
     */
    @Test
    void testGetProjectWorkflowByIdIsDeniedWhenTheOwningProjectIsWithheld() throws Exception {
        seedProjectWorkflow(project(PRIVATE_PROJECT_ID, ResourceVisibility.PRIVATE, OTHER_USER));

        assertThat(evaluateGetProjectWorkflowGuard())
            .as("a colleague's PRIVATE project must not open in the editor by project-workflow id")
            .isFalse();
    }

    /**
     * The companion to the test above, and what makes its {@code false} attributable to visibility: the ONLY difference
     * between the two is the owning project's {@code visibility} column. Same caller, same non-owner {@code createdBy},
     * same ownership resolution (both resolve to a workspace, so {@code hasResourceScope}'s owner branch answers
     * {@code true} in both). Without this case, a {@code false} produced by a broken ownership lookup or a missing
     * provider registration would read as success.
     */
    @Test
    void testGetProjectWorkflowByIdIsAllowedWhenTheOwningProjectIsWorkspaceVisible() throws Exception {
        seedProjectWorkflow(project(WORKSPACE_PROJECT_ID, ResourceVisibility.WORKSPACE, OTHER_USER));

        assertThat(evaluateGetProjectWorkflowGuard())
            .as("workspace reach is what the by-id gate is meant to honour")
            .isTrue();
    }

    @Test
    void testGetProjectWorkflowByIdIsAllowedForTheOwnerOfAPrivateProject() throws Exception {
        seedProjectWorkflow(project(PRIVATE_PROJECT_ID, ResourceVisibility.PRIVATE, USER));

        assertThat(evaluateGetProjectWorkflowGuard())
            .as("PRIVATE means the owner plus grantees, not nobody")
            .isTrue();
    }

    /**
     * The sidebar leak: {@code getProjectWorkflows()} listed every project workflow row in the tenant, so a PRIVATE
     * project's workflow names appeared under "All projects" for every member.
     *
     * <p>
     * Both rows are given a resolvable workflow, so the facade's {@code Objects::nonNull} branch cannot be the branch
     * that drops row 2 — the visibility filter is the only one left.
     */
    @Test
    void testGetProjectWorkflowsHidesTheRowsOfAWithheldProject() {
        CountingResourceVisibilityResolver resourceVisibilityResolver = new CountingResourceVisibilityResolver();

        ProjectWorkflowFacadeImpl projectWorkflowFacade = facade(resourceVisibilityResolver);

        seedTwoProjectsOneWithheld();

        List<ProjectWorkflowDTO> projectWorkflowDTOs = projectWorkflowFacade.getProjectWorkflows();

        assertThat(projectWorkflowDTOs).extracting(ProjectWorkflowDTO::getProjectWorkflowId)
            .containsExactly(10L);
    }

    /**
     * The filter must be consulted ONCE for the whole listing. A per-row check would be an authorization query per
     * project workflow row in the tenant, which is exactly the shape this listing cannot afford.
     */
    @Test
    void testGetProjectWorkflowsResolvesVisibilityInOneBatchedCall() {
        CountingResourceVisibilityResolver resourceVisibilityResolver = new CountingResourceVisibilityResolver();

        ProjectWorkflowFacadeImpl projectWorkflowFacade = facade(resourceVisibilityResolver);

        seedTwoProjectsOneWithheld();

        projectWorkflowFacade.getProjectWorkflows();

        assertThat(resourceVisibilityResolver.candidateCounts)
            .as("one resolver call carrying both projects, not one call per project workflow")
            .containsExactly(2);
    }

    private void seedTwoProjectsOneWithheld() {
        ProjectWorkflow visibleProjectWorkflow = projectWorkflow(10L, WORKSPACE_PROJECT_ID, "wf-visible");
        ProjectWorkflow withheldProjectWorkflow = projectWorkflow(20L, PRIVATE_PROJECT_ID, "wf-withheld");

        when(projectWorkflowService.getProjectWorkflows())
            .thenReturn(List.of(visibleProjectWorkflow, withheldProjectWorkflow));
        when(projectService.getProjects(List.of(WORKSPACE_PROJECT_ID, PRIVATE_PROJECT_ID)))
            .thenReturn(
                List.of(
                    project(WORKSPACE_PROJECT_ID, ResourceVisibility.WORKSPACE, OTHER_USER),
                    project(PRIVATE_PROJECT_ID, ResourceVisibility.PRIVATE, OTHER_USER)));

        // Both rows resolve to a workflow, so the DTO-mapping branch drops neither: whatever is missing from the
        // result is missing because the visibility filter removed it.
        when(workflowFacade.fetchWorkflow("wf-visible")).thenReturn(Optional.of(mock(WorkflowDTO.class)));
        when(workflowFacade.fetchWorkflow("wf-withheld")).thenReturn(Optional.of(mock(WorkflowDTO.class)));
    }

    private void seedProjectWorkflow(Project project) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(project.getId());
        when(projectWorkflowRepository.findById(PROJECT_WORKFLOW_ID)).thenReturn(Optional.of(projectWorkflow));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
    }

    /**
     * Evaluates the guard string read off the implementation's own bytecode rather than a restatement of it, so the
     * test cannot drift from the annotation it claims to verify.
     */
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private boolean evaluateGetProjectWorkflowGuard() throws Exception {
        Method method = ProjectWorkflowFacadeImpl.class.getMethod("getProjectWorkflow", long.class);

        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();

        expressionHandler.setPermissionEvaluator(
            new AutomationPermissionEvaluator(permissionService(), mock(ObjectProvider.class)));

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        SimpleMethodInvocation methodInvocation =
            new SimpleMethodInvocation(new Object(), method, PROJECT_WORKFLOW_ID);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(guardExpression(method));

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static String guardExpression(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("getProjectWorkflow(long) must carry a @PreAuthorize guard")
            .isNotNull();

        return preAuthorize.value();
    }

    /**
     * The real CE permission service over the real {@code ProjectWorkflow} provider and ownership resolver, so the
     * answer comes from production wiring rather than from a stub that could agree with the assertion by accident.
     */
    private PermissionServiceImpl permissionService() {
        List<ResourceOwnershipResolver> resourceOwnershipResolvers = List.of(
            new ProjectWorkflowOwnershipResolver(projectRepository, projectWorkflowRepository));
        List<ResourceVisibilityProvider> resourceVisibilityProviders = List.of(
            new ProjectWorkflowVisibilityProvider(projectRepository, projectWorkflowRepository));

        return new PermissionServiceImpl(
            mock(UserService.class), resourceOwnershipResolvers, resourceVisibilityProviders,
            new DefaultResourceVisibilityResolver());
    }

    private ProjectWorkflowFacadeImpl facade(ResourceVisibilityResolver resourceVisibilityResolver) {
        return new ProjectWorkflowFacadeImpl(
            mock(ComponentDefinitionHelper.class), mock(PreBuiltTemplateService.class),
            mock(ApplicationProperties.class), mock(EnvironmentService.class),
            mock(ErrorWorkflowConfigurationValidator.class), mock(ProjectDeploymentService.class),
            mock(ProjectDeploymentWorkflowService.class), projectService,
            new ProjectVisibilityFilter(objectProvider(resourceVisibilityResolver)), projectWorkflowService,
            mock(SharedTemplateFileStorage.class), mock(SharedTemplateService.class), mock(WorkflowCacheManager.class),
            workflowFacade, List.of(), mock(WorkflowService.class), mock(WorkflowTestConfigurationService.class),
            mock(WorkflowValidatorFacade.class));
    }

    private static void authenticate(String login) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    login, "credentials", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER))));
    }

    private static Project project(long id, ResourceVisibility visibility, String createdBy) {
        Project project = new Project();

        project.setId(id);
        project.setName("project-" + id);
        project.setVisibility(visibility);
        project.setWorkspaceId(WORKSPACE_ID);

        // created_by is @CreatedBy-managed; the test seeds it the way the persistence layer would.
        ReflectionTestUtils.setField(project, "createdBy", createdBy);

        return project;
    }

    private static ProjectWorkflow projectWorkflow(long id, long projectId, String workflowId) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getId()).thenReturn(id);
        when(projectWorkflow.getProjectId()).thenReturn(projectId);
        when(projectWorkflow.getWorkflowId()).thenReturn(workflowId);

        return projectWorkflow;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ResourceVisibilityResolver> objectProvider(
        ResourceVisibilityResolver resourceVisibilityResolver) {

        ObjectProvider<ResourceVisibilityResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(resourceVisibilityResolver);

        return objectProvider;
    }

    /**
     * The real CE resolver, wrapped to record how many candidates each call carried — the batching evidence.
     */
    private static final class CountingResourceVisibilityResolver implements ResourceVisibilityResolver {

        private final List<Integer> candidateCounts = new ArrayList<>();
        private final ResourceVisibilityResolver delegate = new DefaultResourceVisibilityResolver();

        @Override
        public Set<Long> filterVisibleIds(
            String resourceType, long workspaceId, Collection<VisibilityRecord> candidates) {

            candidateCounts.add(candidates.size());

            return delegate.filterVisibleIds(resourceType, workspaceId, candidates);
        }
    }
}
