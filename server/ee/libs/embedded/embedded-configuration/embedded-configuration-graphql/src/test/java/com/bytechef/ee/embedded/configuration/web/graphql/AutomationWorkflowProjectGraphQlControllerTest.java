/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationWorkflowProjectGraphQlControllerTest {

    private static final Answer<Object> BODY_REACHED = invocation -> {
        throw new IllegalStateException("body reached");
    };

    private static final Set<String> ENDPOINT_NAMES = Set.of(
        "automationWorkflowProjectCategories", "automationWorkflowProjectTags", "automationWorkflowProjectVersions",
        "automationWorkflowProjects", "createAutomationWorkflowProject", "createAutomationWorkflowProjectWorkflow",
        "deleteAutomationWorkflowProject", "deleteAutomationWorkflowProjectWorkflow",
        "duplicateAutomationWorkflowProject", "duplicateAutomationWorkflowProjectWorkflow",
        "publishAutomationWorkflowProject", "updateAutomationWorkflowProject",
        "updateAutomationWorkflowProjectWorkflow", "updateAutomationWorkflowProjectWorkflowPermissionExpression");

    private final PermissionService permissionService = mock(PermissionService.class);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

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

    @Test
    void testEveryEndpointIsEvaluated() {
        Set<String> endpointNames = endpointMethods()
            .map(Method::getName)
            .collect(Collectors.toCollection(TreeSet::new));

        assertThat(endpointNames).isEqualTo(new TreeSet<>(ENDPOINT_NAMES));
    }

    /**
     * Enforces the real {@code @PreAuthorize} guard of the query or mutation through the real
     * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. It reads or
     * administers the tenant's embedded catalog, so it must decide on {@link PermissionService#isTenantAdmin()} alone.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointMethods")
    void testEndpointDeniesAUserWhoIsNotATenantAdmin(Method method) {
        authenticate("user@localhost.com", "ROLE_USER", false);

        assertThatThrownBy(() -> invokeSecured(method))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointMethods")
    void testEndpointAdmitsATenantAdmin(Method method) {
        authenticate("admin@localhost.com", "ROLE_ADMIN", true);

        assertThatThrownBy(() -> invokeSecured(method))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("body reached");

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    private void authenticate(String login, String authority, boolean tenantAdmin) {
        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                login, null, List.of(new SimpleGrantedAuthority(authority))));

        SecurityContextHolder.setContext(securityContext);
    }

    private void invokeSecured(Method method) throws Throwable {
        Object securedController = secure(
            new AutomationWorkflowProjectGraphQlController(
                mock(AutomationWorkflowProjectFacade.class, BODY_REACHED)));

        try {
            method.invoke(securedController, createArguments(method));
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    private Object secure(Object target) {
        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return proxyFactory.getProxy();
    }

    private static Object[] createArguments(Executable executable) throws ReflectiveOperationException {
        Class<?>[] parameterTypes = executable.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];

        for (int index = 0; index < parameterTypes.length; index++) {
            arguments[index] = createArgument(parameterTypes[index]);
        }

        return arguments;
    }

    private static Object createArgument(Class<?> parameterType) throws ReflectiveOperationException {
        if (parameterType == String.class) {
            return "1";
        } else if (parameterType == Long.class || parameterType == long.class) {
            return 1L;
        } else if (parameterType == Integer.class || parameterType == int.class) {
            return 1;
        } else if (parameterType == Boolean.class || parameterType == boolean.class) {
            return true;
        } else if (parameterType == List.class) {
            return List.of();
        } else if (parameterType == Map.class) {
            return Map.of();
        } else if (parameterType.isRecord()) {
            Constructor<?> canonicalConstructor = parameterType.getDeclaredConstructor(
                Arrays.stream(parameterType.getRecordComponents())
                    .map(RecordComponent::getType)
                    .toArray(Class<?>[]::new));

            canonicalConstructor.setAccessible(true);

            return canonicalConstructor.newInstance(createArguments(canonicalConstructor));
        } else if (parameterType.isEnum()) {
            return parameterType.getEnumConstants()[0];
        } else if (parameterType.isPrimitive()) {
            return Array.get(Array.newInstance(parameterType, 1), 0);
        } else if (parameterType.getName()
            .startsWith("com.bytechef.")) {
            return parameterType.getDeclaredConstructor()
                .newInstance();
        }

        return null;
    }

    private static Stream<Method> endpointMethods() {
        return Arrays.stream(AutomationWorkflowProjectGraphQlController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(MutationMapping.class) ||
                method.isAnnotationPresent(QueryMapping.class));
    }
}
