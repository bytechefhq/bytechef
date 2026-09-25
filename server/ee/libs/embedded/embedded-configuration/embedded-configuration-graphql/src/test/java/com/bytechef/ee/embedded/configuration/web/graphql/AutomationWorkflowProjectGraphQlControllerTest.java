/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationWorkflowProjectGraphQlControllerTest {

    private static final Set<String> ENDPOINT_NAMES = Set.of(
        "automationWorkflowProjectCategories", "automationWorkflowProjectTags", "automationWorkflowProjectVersions",
        "automationWorkflowProjects", "createAutomationWorkflowProject", "createAutomationWorkflowProjectWorkflow",
        "deleteAutomationWorkflowProject", "deleteAutomationWorkflowProjectWorkflow",
        "duplicateAutomationWorkflowProject", "duplicateAutomationWorkflowProjectWorkflow",
        "publishAutomationWorkflowProject", "updateAutomationWorkflowProject",
        "updateAutomationWorkflowProjectWorkflow", "updateAutomationWorkflowProjectWorkflowPermissionExpression");

    @Test
    void testAutomationWorkflowProjectsReturnsListFromFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        ConnectedUserWorkflowTemplateDTO workflowTemplateOne = new ConnectedUserWorkflowTemplateDTO(
            "wf-uuid-1", "Workflow One", "First workflow", "2026-05-22T10:00:00Z",
            List.of(new ConnectedUserWorkflowTemplateDTO.Component("manual", "Manual Trigger", "manual-icon")),
            List.of(new ConnectedUserWorkflowTemplateDTO.Component("gmail", "Gmail", "gmail-icon")), null);

        AutomationWorkflowProjectDTO projectOne =
            new AutomationWorkflowProjectDTO(1L, "Project One", "First project", null, List.of(), true, 5, 2,
                List.of(workflowTemplateOne), null);
        AutomationWorkflowProjectDTO projectTwo =
            new AutomationWorkflowProjectDTO(2L, "Project Two", null, 10L, List.of(20L), false, 1, null, List.of(),
                null);

        when(automationWorkflowProjectFacade.getProjects()).thenReturn(List.of(projectOne, projectTwo));

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        List<AutomationWorkflowProjectDTO> result = controller.automationWorkflowProjects();

        assertThat(result).hasSize(2);
        AutomationWorkflowProjectDTO workflowProjectDTO = result.getFirst();

        assertThat(workflowProjectDTO.id()).isEqualTo(1L);
        assertThat(workflowProjectDTO.name()).isEqualTo("Project One");
        assertThat(workflowProjectDTO.description()).isEqualTo("First project");
        assertThat(workflowProjectDTO.categoryId()).isNull();

        List<ConnectedUserWorkflowTemplateDTO> connectedUserWorkflowTemplateDTOs1 =
            workflowProjectDTO.workflowTemplates();

        assertThat(connectedUserWorkflowTemplateDTOs1).hasSize(1);
        assertThat(workflowProjectDTO.version()).isEqualTo(5);

        ConnectedUserWorkflowTemplateDTO connectedUserWorkflowTemplateDTO = connectedUserWorkflowTemplateDTOs1
            .getFirst();

        assertThat(connectedUserWorkflowTemplateDTO.lastModifiedDate()).isEqualTo("2026-05-22T10:00:00Z");

        AutomationWorkflowProjectDTO automationWorkflowProjectDTO = result.getFirst();

        List<ConnectedUserWorkflowTemplateDTO> connectedUserWorkflowTemplateDTOs2 = automationWorkflowProjectDTO
            .workflowTemplates();

        ConnectedUserWorkflowTemplateDTO connectedUserWorkflowTemplateDTO2 = connectedUserWorkflowTemplateDTOs2
            .getFirst();

        assertThat(connectedUserWorkflowTemplateDTO2.triggers()).hasSize(1);
        assertThat(connectedUserWorkflowTemplateDTO2.components()).hasSize(1);

        AutomationWorkflowProjectDTO automationWorkflowProjectDTO2 = result.get(1);

        assertThat(automationWorkflowProjectDTO2.id()).isEqualTo(2L);
        assertThat(automationWorkflowProjectDTO2.description()).isNull();
        assertThat(automationWorkflowProjectDTO2.categoryId()).isEqualTo(10L);
    }

    @Test
    void testCreateAutomationWorkflowProjectDelegatesToFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        when(automationWorkflowProjectFacade.createProject("My Project", "desc", null, List.of(), null))
            .thenReturn(42L);

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        String result = controller.createAutomationWorkflowProject("My Project", "desc", null, null, null);

        assertThat(result).isEqualTo("42");
        verify(automationWorkflowProjectFacade).createProject("My Project", "desc", null, List.of(), null);
    }

    @Test
    void testCreateAutomationWorkflowProjectWithCategoryAndTags() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        when(automationWorkflowProjectFacade.createProject("P", null, "Electronics", List.of("java", "spring"), null))
            .thenReturn(99L);

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        String result =
            controller.createAutomationWorkflowProject("P", null, "Electronics", List.of("java", "spring"), null);

        assertThat(result).isEqualTo("99");
        verify(automationWorkflowProjectFacade).createProject("P", null, "Electronics", List.of("java", "spring"),
            null);
    }

    @Test
    void testUpdateAutomationWorkflowProjectDelegatesToFacadeAndReturnsTrue() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        boolean result =
            controller.updateAutomationWorkflowProject("7", "Updated", "new desc", "Finance", List.of("api"), null);

        assertThat(result).isTrue();
        verify(automationWorkflowProjectFacade).updateProject(
            7L, "Updated", "new desc", "Finance", List.of("api"), null);
    }

    @Test
    void testDeleteAutomationWorkflowProjectDelegatesToFacadeAndReturnsTrue() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        boolean result = controller.deleteAutomationWorkflowProject("5");

        assertThat(result).isTrue();
        verify(automationWorkflowProjectFacade).deleteProject(5L);
    }

    @Test
    void testCreateAutomationWorkflowProjectWorkflowDelegatesToFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        when(automationWorkflowProjectFacade.createProjectWorkflow(3L, "{\"tasks\":[]}", null))
            .thenReturn("new-wf-uuid");

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        String result = controller.createAutomationWorkflowProjectWorkflow("3", "{\"tasks\":[]}", null);

        assertThat(result).isEqualTo("new-wf-uuid");
        verify(automationWorkflowProjectFacade).createProjectWorkflow(3L, "{\"tasks\":[]}", null);
    }

    @Test
    void testDeleteAutomationWorkflowProjectWorkflowDelegatesToFacadeAndReturnsTrue() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        boolean result = controller.deleteAutomationWorkflowProjectWorkflow("wf-uuid-to-delete");

        assertThat(result).isTrue();
        verify(automationWorkflowProjectFacade).deleteProjectWorkflow("wf-uuid-to-delete");
    }

    @Test
    void testPublishAutomationWorkflowProjectDelegatesToFacadeAndReturnsTrue() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        boolean result = controller.publishAutomationWorkflowProject("8");

        assertThat(result).isTrue();
        verify(automationWorkflowProjectFacade).publishProject(8L);
    }

    @Test
    void testAutomationWorkflowProjectCategoriesReturnsCategoriesFromFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectCategoryDTO categoryOne = new AutomationWorkflowProjectCategoryDTO(1L, "Finance");
        AutomationWorkflowProjectCategoryDTO categoryTwo = new AutomationWorkflowProjectCategoryDTO(2L, "Marketing");

        when(automationWorkflowProjectFacade.getCategories()).thenReturn(List.of(categoryOne, categoryTwo));

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        List<AutomationWorkflowProjectCategoryDTO> result = controller.automationWorkflowProjectCategories();

        assertThat(result).hasSize(2);

        AutomationWorkflowProjectCategoryDTO automationWorkflowProjectCategoryDTO1 = result.getFirst();

        assertThat(automationWorkflowProjectCategoryDTO1.id()).isEqualTo(1L);
        assertThat(automationWorkflowProjectCategoryDTO1.name()).isEqualTo("Finance");

        AutomationWorkflowProjectCategoryDTO automationWorkflowProjectCategoryDTO2 = result.get(1);

        assertThat(automationWorkflowProjectCategoryDTO2.id()).isEqualTo(2L);
        assertThat(automationWorkflowProjectCategoryDTO2.name()).isEqualTo("Marketing");
    }

    @Test
    void testAutomationWorkflowProjectTagsReturnsTagsFromFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectTagDTO tagOne = new AutomationWorkflowProjectTagDTO(10L, "java");
        AutomationWorkflowProjectTagDTO tagTwo = new AutomationWorkflowProjectTagDTO(20L, "spring");

        when(automationWorkflowProjectFacade.getTags()).thenReturn(List.of(tagOne, tagTwo));

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        List<AutomationWorkflowProjectTagDTO> result = controller.automationWorkflowProjectTags();

        assertThat(result).hasSize(2);
        AutomationWorkflowProjectTagDTO automationWorkflowProjectTagDTO1 = result.getFirst();

        assertThat(automationWorkflowProjectTagDTO1.id()).isEqualTo(10L);
        assertThat(automationWorkflowProjectTagDTO1.name()).isEqualTo("java");

        AutomationWorkflowProjectTagDTO automationWorkflowProjectTagDTO2 = result.get(1);

        assertThat(automationWorkflowProjectTagDTO2.id()).isEqualTo(20L);
        assertThat(automationWorkflowProjectTagDTO2.name()).isEqualTo("spring");
    }

    @Test
    void testAutomationWorkflowProjectVersionsReturnsVersionsFromFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectVersionDTO versionOne = new AutomationWorkflowProjectVersionDTO(
            1, "PUBLISHED", "2026-01-01T10:00:00Z");
        AutomationWorkflowProjectVersionDTO versionTwo = new AutomationWorkflowProjectVersionDTO(2, "DRAFT", null);

        when(automationWorkflowProjectFacade.getProjectVersions(5L)).thenReturn(List.of(versionOne, versionTwo));

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        List<AutomationWorkflowProjectVersionDTO> result = controller.automationWorkflowProjectVersions("5");

        assertThat(result).hasSize(2);
        AutomationWorkflowProjectVersionDTO automationWorkflowProjectVersionDTO1 = result.getFirst();

        assertThat(automationWorkflowProjectVersionDTO1.version()).isEqualTo(1);
        assertThat(automationWorkflowProjectVersionDTO1.status()).isEqualTo("PUBLISHED");
        assertThat(automationWorkflowProjectVersionDTO1.publishedDate()).isEqualTo("2026-01-01T10:00:00Z");

        AutomationWorkflowProjectVersionDTO automationWorkflowProjectVersionDTO2 = result.get(1);

        assertThat(automationWorkflowProjectVersionDTO2.version()).isEqualTo(2);
        assertThat(automationWorkflowProjectVersionDTO2.status()).isEqualTo("DRAFT");
        assertThat(automationWorkflowProjectVersionDTO2.publishedDate()).isNull();

        verify(automationWorkflowProjectFacade).getProjectVersions(5L);
    }

    @Test
    void testDuplicateAutomationWorkflowProjectWorkflowDelegatesToFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        when(automationWorkflowProjectFacade.duplicateProjectWorkflow("source-wf-id")).thenReturn("new-wf-id");

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        String result = controller.duplicateAutomationWorkflowProjectWorkflow("source-wf-id");

        assertThat(result).isEqualTo("new-wf-id");
        verify(automationWorkflowProjectFacade).duplicateProjectWorkflow("source-wf-id");
    }

    @Test
    void testDuplicateAutomationWorkflowProjectDelegatesToFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        when(automationWorkflowProjectFacade.duplicateProject(3L)).thenReturn(99L);

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        String result = controller.duplicateAutomationWorkflowProject("3");

        assertThat(result).isEqualTo("99");
        verify(automationWorkflowProjectFacade).duplicateProject(3L);
    }

    static Stream<Arguments> endpoints() {
        return endpointMethods().flatMap(method -> Stream.of(Arguments.of(method, false), Arguments.of(method, true)));
    }

    @Test
    void testEveryEndpointIsEvaluated() {
        Set<String> endpointNames = endpointMethods()
            .map(Method::getName)
            .collect(Collectors.toCollection(TreeSet::new));

        assertThat(endpointNames).isEqualTo(new TreeSet<>(ENDPOINT_NAMES));
    }

    /**
     * Evaluates the real {@code @PreAuthorize} expression of the query or mutation through the real
     * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. It reads or
     * administers the tenant's embedded catalog, so it must decide on {@link PermissionService#isTenantAdmin()} alone.
     */
    @ParameterizedTest(name = "{0} tenantAdmin={1}")
    @MethodSource("endpoints")
    void testEndpointRequiresATenantAdmin(Method method, boolean tenantAdmin) {
        assertThat(Modifier.isPublic(method.getModifiers()))
            .as("%s must be public, since a proxy only enforces a public guard", method.getName())
            .isTrue();

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

        assertThat(evaluateGuard(permissionService, method))
            .as("%s must %s when isTenantAdmin() returns %s", method.getName(), tenantAdmin ? "allow" : "deny",
                tenantAdmin)
            .isEqualTo(tenantAdmin);

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, new Object[method.getParameterCount()]);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Stream<Method> endpointMethods() {
        return Arrays.stream(AutomationWorkflowProjectGraphQlController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(MutationMapping.class) ||
                method.isAnnotationPresent(QueryMapping.class));
    }
}
