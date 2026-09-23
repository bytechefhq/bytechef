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

package com.bytechef.automation.knowledgebase.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.knowledgebase.web.graphql.KnowledgeBaseDocumentTagGraphQlController.UpdateKnowledgeBaseDocumentTagsInput;
import com.bytechef.automation.knowledgebase.web.graphql.KnowledgeBaseTagGraphQlController.UpdateKnowledgeBaseTagsInput;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expression on every user-facing knowledge base GraphQL operation through the
 * real {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}, asserting each
 * guard in both directions and the exact check that reaches {@link PermissionService}. The guards sit on the
 * controllers rather than on the platform knowledge base services, because the knowledge base component and the
 * ingestion worker call those services at runtime, where no user is present to hold a scope.
 *
 * @author Ivica Cardic
 */
class KnowledgeBaseGraphQlControllerAuthorizationTest {

    private static final long CHUNK_ID = 13L;
    private static final long DOCUMENT_ID = 12L;
    private static final long ENVIRONMENT_ID = 2L;
    private static final List<Class<?>> GUARDED_CONTROLLER_CLASSES = List.of(
        KnowledgeBaseDocumentChunkGraphQlController.class, KnowledgeBaseDocumentGraphQlController.class,
        KnowledgeBaseDocumentTagGraphQlController.class, KnowledgeBaseGraphQlController.class,
        KnowledgeBaseTagGraphQlController.class);
    private static final long KNOWLEDGE_BASE_ID = 11L;
    private static final Environment PRODUCTION = Environment.PRODUCTION;
    private static final long WORKSPACE_ID = 42L;

    @ParameterizedTest(name = "{0} granted={1}")
    @MethodSource("guardCases")
    void testGuardDecidesOnTheExpectedPermissionCheck(GuardCase guardCase, boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);
        Function<PermissionService, Boolean> expectedCheck = guardCase.expectedCheck();

        when(expectedCheck.apply(permissionService)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, guardCase))
            .as("%s must %s when its permission check returns %s", guardCase, granted ? "allow" : "deny", granted)
            .isEqualTo(granted);

        expectedCheck.apply(verify(permissionService));
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testEveryGuardedMethodHasAnEvaluatedCase() {
        Set<String> guardedMethodNames = GUARDED_CONTROLLER_CLASSES.stream()
            .flatMap(controllerClass -> Arrays.stream(controllerClass.getDeclaredMethods()))
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .map(method -> method.getDeclaringClass()
                .getSimpleName() + "#" + method.getName())
            .collect(Collectors.toSet());

        Set<String> evaluatedMethodNames = guardCaseStream()
            .map(GuardCase::toString)
            .collect(Collectors.toSet());

        assertThat(evaluatedMethodNames).isEqualTo(guardedMethodNames);
    }

    static Stream<Arguments> guardCases() {
        return guardCaseStream()
            .flatMap(guardCase -> Stream.of(Arguments.of(guardCase, false), Arguments.of(guardCase, true)));
    }

    private static Stream<GuardCase> guardCaseStream() {
        Map<String, Object> workspaceArguments = Map.of("environmentId", ENVIRONMENT_ID, "workspaceId", WORKSPACE_ID);

        return Stream.of(
            new GuardCase(
                KnowledgeBaseTagGraphQlController.class, "knowledgeBaseTags", workspaceArguments,
                permissionService -> permissionService.hasWorkspaceScope(
                    WORKSPACE_ID, "KNOWLEDGE_BASE_VIEW", PRODUCTION)),
            new GuardCase(
                KnowledgeBaseTagGraphQlController.class, "knowledgeBaseTagsByKnowledgeBase", workspaceArguments,
                permissionService -> permissionService.hasWorkspaceScope(
                    WORKSPACE_ID, "KNOWLEDGE_BASE_VIEW", PRODUCTION)),
            new GuardCase(
                KnowledgeBaseTagGraphQlController.class, "updateKnowledgeBaseTags",
                Map.of("input", new UpdateKnowledgeBaseTagsInput(KNOWLEDGE_BASE_ID, List.of())),
                knowledgeBaseCheck("KNOWLEDGE_BASE_EDIT")),
            new GuardCase(
                KnowledgeBaseGraphQlController.class, "knowledgeBase", Map.of("id", KNOWLEDGE_BASE_ID),
                knowledgeBaseCheck("KNOWLEDGE_BASE_VIEW")),
            new GuardCase(
                KnowledgeBaseGraphQlController.class, "searchKnowledgeBase",
                Map.of("id", KNOWLEDGE_BASE_ID, "query", "invoices"), knowledgeBaseCheck("KNOWLEDGE_BASE_VIEW")),
            new GuardCase(
                KnowledgeBaseGraphQlController.class, "updateKnowledgeBase", Map.of("id", KNOWLEDGE_BASE_ID),
                knowledgeBaseCheck("KNOWLEDGE_BASE_EDIT")),
            new GuardCase(
                KnowledgeBaseDocumentGraphQlController.class, "knowledgeBaseDocument", Map.of("id", DOCUMENT_ID),
                documentCheck("KNOWLEDGE_BASE_VIEW")),
            new GuardCase(
                KnowledgeBaseDocumentGraphQlController.class, "knowledgeBaseDocumentChunks",
                Map.of("id", DOCUMENT_ID), documentCheck("KNOWLEDGE_BASE_VIEW")),
            new GuardCase(
                KnowledgeBaseDocumentGraphQlController.class, "knowledgeBaseDocumentStatus",
                Map.of("id", DOCUMENT_ID), documentCheck("KNOWLEDGE_BASE_VIEW")),
            new GuardCase(
                KnowledgeBaseDocumentGraphQlController.class, "deleteKnowledgeBaseDocument",
                Map.of("id", DOCUMENT_ID), documentCheck("KNOWLEDGE_BASE_EDIT")),
            new GuardCase(
                KnowledgeBaseDocumentTagGraphQlController.class, "knowledgeBaseDocumentTags",
                Map.of("knowledgeBaseId", KNOWLEDGE_BASE_ID), knowledgeBaseCheck("KNOWLEDGE_BASE_VIEW")),
            new GuardCase(
                KnowledgeBaseDocumentTagGraphQlController.class, "knowledgeBaseDocumentTagsByDocument",
                Map.of("knowledgeBaseId", KNOWLEDGE_BASE_ID), knowledgeBaseCheck("KNOWLEDGE_BASE_VIEW")),
            new GuardCase(
                KnowledgeBaseDocumentTagGraphQlController.class, "updateKnowledgeBaseDocumentTags",
                Map.of("input", new UpdateKnowledgeBaseDocumentTagsInput(DOCUMENT_ID, List.of())),
                documentCheck("KNOWLEDGE_BASE_EDIT")),
            new GuardCase(
                KnowledgeBaseDocumentChunkGraphQlController.class, "updateKnowledgeBaseDocumentChunk",
                Map.of("id", CHUNK_ID), chunkCheck()),
            new GuardCase(
                KnowledgeBaseDocumentChunkGraphQlController.class, "deleteKnowledgeBaseDocumentChunk",
                Map.of("id", CHUNK_ID), chunkCheck()));
    }

    private static Function<PermissionService, Boolean> chunkCheck() {
        return permissionService -> permissionService.hasResourceScope(
            CHUNK_ID, "KnowledgeBaseDocumentChunk", "KNOWLEDGE_BASE_EDIT");
    }

    private static Function<PermissionService, Boolean> documentCheck(String scope) {
        return permissionService -> permissionService.hasResourceScope(DOCUMENT_ID, "KnowledgeBaseDocument", scope);
    }

    private static Function<PermissionService, Boolean> knowledgeBaseCheck(String scope) {
        return permissionService -> permissionService.hasResourceScope(KNOWLEDGE_BASE_ID, "KnowledgeBase", scope);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, GuardCase guardCase) {
        Method method = findMethod(guardCase.controllerClass(), guardCase.methodName());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, toArguments(method, guardCase.argumentsByName()));

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method findMethod(Class<?> controllerClass, String methodName) {
        List<Method> methods = Arrays.stream(controllerClass.getDeclaredMethods())
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
        Class<?> controllerClass, String methodName, Map<String, Object> argumentsByName,
        Function<PermissionService, Boolean> expectedCheck) {

        @Override
        public String toString() {
            return controllerClass.getSimpleName() + "#" + methodName;
        }
    }
}
