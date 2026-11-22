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

package com.bytechef.automation.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseTagFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseStorageService;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Calls the guarded methods of {@link WorkspaceKnowledgeBaseFacadeImpl} and
 * {@link WorkspaceKnowledgeBaseDocumentFacadeImpl} through the real Spring method-security interceptor, backed by the
 * real {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}, asserting each
 * guard in both directions and the exact check that reaches {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
class WorkspaceKnowledgeBaseFacadeTest {

    private static final String BODY_REACHED = "body reached";
    private static final long ENVIRONMENT_ID = 2L;
    private static final long KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID = 31L;
    private static final long KNOWLEDGE_BASE_DOCUMENT_ID = 21L;
    private static final long KNOWLEDGE_BASE_ID = 11L;
    private static final Environment PRODUCTION = Environment.PRODUCTION;
    private static final long WORKSPACE_ID = 42L;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("member", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{0} granted={1}")
    @MethodSource("guardCases")
    void testGuardDecidesOnTheExpectedPermissionCheck(GuardCase guardCase, boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);
        Function<PermissionService, Boolean> expectedCheck = guardCase.expectedCheck();

        when(expectedCheck.apply(permissionService)).thenReturn(granted);

        Method method = findMethod(guardCase.facadeClass(), guardCase.methodName());
        Object[] arguments = toArguments(method, guardCase.argumentsByName());
        Object facade = secure(newFacade(guardCase.facadeClass()), permissionService);

        if (granted) {
            assertThatThrownBy(() -> invoke(method, facade, arguments))
                .as("%s must reach its body when its permission check grants", guardCase)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(() -> invoke(method, facade, arguments))
                .as("%s must be denied when its permission check refuses", guardCase)
                .isInstanceOf(AccessDeniedException.class);
        }

        expectedCheck.apply(verify(permissionService));
        verifyNoMoreInteractions(permissionService);
    }

    @ParameterizedTest
    @MethodSource("facadeClasses")
    void testEveryGuardedMethodHasAnEvaluatedCase(Class<?> facadeClass) {
        Set<String> guardedMethodNames = Arrays.stream(facadeClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .map(Method::getName)
            .collect(Collectors.toSet());

        Set<String> evaluatedMethodNames = guardCaseStream()
            .filter(guardCase -> guardCase.facadeClass() == facadeClass)
            .map(GuardCase::methodName)
            .collect(Collectors.toSet());

        assertThat(evaluatedMethodNames).isEqualTo(guardedMethodNames);
    }

    static Stream<Class<?>> facadeClasses() {
        return Stream.of(WorkspaceKnowledgeBaseFacadeImpl.class, WorkspaceKnowledgeBaseDocumentFacadeImpl.class);
    }

    static Stream<Arguments> guardCases() {
        return guardCaseStream()
            .flatMap(guardCase -> Stream.of(Arguments.of(guardCase, false), Arguments.of(guardCase, true)));
    }

    private static Stream<GuardCase> guardCaseStream() {
        Map<String, Object> workspaceArguments = Map.of("environmentId", ENVIRONMENT_ID, "workspaceId", WORKSPACE_ID);

        return Stream.of(
            workspaceCase("getWorkspaceKnowledgeBases", workspaceArguments, "KNOWLEDGE_BASE_VIEW"),
            workspaceCase("getWorkspaceKnowledgeBaseTags", workspaceArguments, "KNOWLEDGE_BASE_VIEW"),
            workspaceCase("getWorkspaceKnowledgeBaseTagsByKnowledgeBaseId", workspaceArguments, "KNOWLEDGE_BASE_VIEW"),
            workspaceCase(
                "createWorkspaceKnowledgeBase",
                Map.of(
                    "environmentId", ENVIRONMENT_ID, "knowledgeBase", new KnowledgeBase(), "workspaceId", WORKSPACE_ID),
                "KNOWLEDGE_BASE_CREATE"),
            workspaceCase(
                "cloneWorkspaceKnowledgeBase",
                Map.of(
                    "knowledgeBaseId", KNOWLEDGE_BASE_ID, "newName", "clone", "targetEnvironmentId", ENVIRONMENT_ID,
                    "workspaceId", WORKSPACE_ID),
                "KNOWLEDGE_BASE_CREATE"),
            knowledgeBaseCase(
                "deleteWorkspaceKnowledgeBase", Map.of("knowledgeBaseId", KNOWLEDGE_BASE_ID), "KNOWLEDGE_BASE_DELETE"),
            knowledgeBaseCase("getKnowledgeBase", Map.of("knowledgeBaseId", KNOWLEDGE_BASE_ID), "KNOWLEDGE_BASE_VIEW"),
            knowledgeBaseCase(
                "searchKnowledgeBase",
                Map.of("knowledgeBaseId", KNOWLEDGE_BASE_ID, "metadataFilters", "", "query", "query"),
                "KNOWLEDGE_BASE_VIEW"),
            knowledgeBaseCase(
                "updateKnowledgeBase",
                Map.of("knowledgeBase", new KnowledgeBase(), "knowledgeBaseId", KNOWLEDGE_BASE_ID),
                "KNOWLEDGE_BASE_EDIT"),
            knowledgeBaseCase(
                "updateKnowledgeBaseTags", Map.of("knowledgeBaseId", KNOWLEDGE_BASE_ID, "tags", List.of()),
                "KNOWLEDGE_BASE_EDIT"),
            documentFacadeKnowledgeBaseCase(
                "getKnowledgeBaseDocumentTags", Map.of("knowledgeBaseId", KNOWLEDGE_BASE_ID), "KNOWLEDGE_BASE_VIEW"),
            documentFacadeKnowledgeBaseCase(
                "getKnowledgeBaseDocumentTagsByDocumentId", Map.of("knowledgeBaseId", KNOWLEDGE_BASE_ID),
                "KNOWLEDGE_BASE_VIEW"),
            documentCase(
                "getKnowledgeBaseDocument", Map.of("knowledgeBaseDocumentId", KNOWLEDGE_BASE_DOCUMENT_ID),
                "KNOWLEDGE_BASE_VIEW"),
            documentCase(
                "getKnowledgeBaseDocumentChunks", Map.of("knowledgeBaseDocumentId", KNOWLEDGE_BASE_DOCUMENT_ID),
                "KNOWLEDGE_BASE_VIEW"),
            documentCase(
                "getKnowledgeBaseDocumentStatus", Map.of("knowledgeBaseDocumentId", KNOWLEDGE_BASE_DOCUMENT_ID),
                "KNOWLEDGE_BASE_VIEW"),
            documentCase(
                "deleteKnowledgeBaseDocument", Map.of("knowledgeBaseDocumentId", KNOWLEDGE_BASE_DOCUMENT_ID),
                "KNOWLEDGE_BASE_EDIT"),
            documentCase(
                "updateKnowledgeBaseDocumentTags",
                Map.of("knowledgeBaseDocumentId", KNOWLEDGE_BASE_DOCUMENT_ID, "tagNames", List.of()),
                "KNOWLEDGE_BASE_EDIT"),
            chunkCase(
                "updateKnowledgeBaseDocumentChunk",
                Map.of("content", "content", "knowledgeBaseDocumentChunkId", KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID)),
            chunkCase(
                "deleteKnowledgeBaseDocumentChunk",
                Map.of("knowledgeBaseDocumentChunkId", KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID)));
    }

    private static GuardCase workspaceCase(String methodName, Map<String, Object> arguments, String scope) {
        return new GuardCase(
            WorkspaceKnowledgeBaseFacadeImpl.class, methodName, arguments,
            permissionService -> permissionService.hasWorkspaceScope(WORKSPACE_ID, scope, PRODUCTION));
    }

    private static GuardCase knowledgeBaseCase(String methodName, Map<String, Object> arguments, String scope) {
        return new GuardCase(
            WorkspaceKnowledgeBaseFacadeImpl.class, methodName, arguments,
            permissionService -> permissionService.hasResourceScope(KNOWLEDGE_BASE_ID, "KnowledgeBase", scope));
    }

    private static GuardCase documentFacadeKnowledgeBaseCase(
        String methodName, Map<String, Object> arguments, String scope) {

        return new GuardCase(
            WorkspaceKnowledgeBaseDocumentFacadeImpl.class, methodName, arguments,
            permissionService -> permissionService.hasResourceScope(KNOWLEDGE_BASE_ID, "KnowledgeBase", scope));
    }

    private static GuardCase documentCase(String methodName, Map<String, Object> arguments, String scope) {
        return new GuardCase(
            WorkspaceKnowledgeBaseDocumentFacadeImpl.class, methodName, arguments,
            permissionService -> permissionService.hasResourceScope(
                KNOWLEDGE_BASE_DOCUMENT_ID, "KnowledgeBaseDocument", scope));
    }

    private static GuardCase chunkCase(String methodName, Map<String, Object> arguments) {
        return new GuardCase(
            WorkspaceKnowledgeBaseDocumentFacadeImpl.class, methodName, arguments,
            permissionService -> permissionService.hasResourceScope(
                KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, "KnowledgeBaseDocumentChunk", "KNOWLEDGE_BASE_EDIT"));
    }

    private static Object newFacade(Class<?> facadeClass) {
        if (facadeClass == WorkspaceKnowledgeBaseDocumentFacadeImpl.class) {
            return new WorkspaceKnowledgeBaseDocumentFacadeImpl(
                bodyReachedMock(KnowledgeBaseDocumentChunkFacade.class),
                bodyReachedMock(KnowledgeBaseDocumentFacade.class), bodyReachedMock(KnowledgeBaseDocumentService.class),
                bodyReachedMock(KnowledgeBaseDocumentTagService.class));
        }

        return new WorkspaceKnowledgeBaseFacadeImpl(
            bodyReachedMock(KnowledgeBaseDocumentFacade.class), bodyReachedMock(KnowledgeBaseDocumentService.class),
            bodyReachedMock(KnowledgeBaseFacade.class), bodyReachedMock(KnowledgeBaseService.class),
            bodyReachedMock(KnowledgeBaseStorageService.class), bodyReachedMock(KnowledgeBaseTagFacade.class),
            bodyReachedMock(WorkspaceKnowledgeBaseService.class));
    }

    private static Object invoke(Method method, Object target, Object[] arguments) throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    private static <T> T bodyReachedMock(Class<T> type) {
        return mock(type, invocation -> {
            throw new IllegalStateException(BODY_REACHED);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T secure(T target, PermissionService permissionService) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        ObjectProvider<MethodSecurityExpressionContributor> contributorProvider =
            beanFactory.getBeanProvider(MethodSecurityExpressionContributor.class);

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService, contributorProvider);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize(
            preAuthorizeAuthorizationManager));

        return (T) proxyFactory.getProxy();
    }

    private static Method findMethod(Class<?> facadeClass, String methodName) {
        List<Method> methods = Arrays.stream(facadeClass.getDeclaredMethods())
            .filter(method -> !method.isSynthetic() && Modifier.isPublic(method.getModifiers()))
            .filter(method -> methodName.equals(method.getName()))
            .toList();

        assertThat(methods)
            .as("Expected exactly one public '%s' method, since a proxy only enforces a public guard", methodName)
            .hasSize(1);

        return methods.getFirst();
    }

    private static Object[] toArguments(Method method, Map<String, Object> argumentsByName) {
        Parameter[] parameters = method.getParameters();

        List<String> parameterNames = Arrays.stream(parameters)
            .map(Parameter::getName)
            .toList();

        assertThat(parameterNames)
            .as("%s must declare every parameter its guard is evaluated with", method.getName())
            .containsAll(argumentsByName.keySet());

        Object[] arguments = new Object[parameters.length];

        for (int index = 0; index < parameters.length; index++) {
            arguments[index] = argumentsByName.get(parameterNames.get(index));
        }

        return arguments;
    }

    private record GuardCase(
        Class<?> facadeClass, String methodName, Map<String, Object> argumentsByName,
        Function<PermissionService, Boolean> expectedCheck) {

        @Override
        public String toString() {
            return methodName;
        }
    }
}
