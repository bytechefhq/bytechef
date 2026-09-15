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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlConfigurationSharedMocks;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlTestConfiguration;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration tests for {@link KnowledgeBaseGraphQlController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationKnowledgeBaseGraphQlTestConfiguration.class,
    KnowledgeBaseGraphQlController.class,
    KnowledgeBaseDocumentGraphQlController.class,
    KnowledgeBaseDocumentChunkGraphQlController.class
})
@GraphQlTest(
    controllers = {
        KnowledgeBaseGraphQlController.class,
        KnowledgeBaseDocumentGraphQlController.class,
        KnowledgeBaseDocumentChunkGraphQlController.class
    },
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.ai.knowledge-base.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
@AutomationKnowledgeBaseGraphQlConfigurationSharedMocks
class KnowledgeBaseGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Autowired
    private KnowledgeBaseFacade knowledgeBaseFacade;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;

    @Test
    void testGetKnowledgeBases() {
        Long workspaceId = 1L;
        long environmentId = 0L;
        List<KnowledgeBase> mockKnowledgeBases = List.of(
            createMockKnowledgeBase(1L, "KnowledgeBase 1"),
            createMockKnowledgeBase(2L, "KnowledgeBase 2"));

        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(workspaceId, environmentId))
            .thenReturn(mockKnowledgeBases);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBases(environmentId: "0", workspaceId: "1") {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("knowledgeBases")
            .entityList(Object.class)
            .hasSize(2);

        verify(workspaceKnowledgeBaseFacade).getWorkspaceKnowledgeBases(workspaceId, environmentId);
    }

    @Test
    void testGetKnowledgeBase() {
        Long knowledgeBaseId = 1L;
        KnowledgeBase mockKnowledgeBase = createMockKnowledgeBase(knowledgeBaseId, "Test KnowledgeBase");

        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(mockKnowledgeBase);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBase(id: "1") {
                        id
                        name
                        description
                    }
                }
                """)
            .execute()
            .path("knowledgeBase.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("knowledgeBase.name")
            .entity(String.class)
            .isEqualTo("Test KnowledgeBase");

        verify(knowledgeBaseService).getKnowledgeBase(knowledgeBaseId);
    }

    @Test
    void testSearchKnowledgeBase() {
        Long knowledgeBaseId = 1L;
        String query = "test query";

        List<KnowledgeBaseDocumentChunk> mockChunks = List.of(
            createMockChunk(1L, "chunk content 1"),
            createMockChunk(2L, "chunk content 2"));

        when(knowledgeBaseFacade.searchKnowledgeBase(eq(knowledgeBaseId), eq(query), any())).thenReturn(mockChunks);

        this.graphQlTester
            .document("""
                query {
                    searchKnowledgeBase(id: "1", query: "test query") {
                        id
                    }
                }
                """)
            .execute()
            .path("searchKnowledgeBase")
            .entityList(Object.class)
            .hasSize(2);

        verify(knowledgeBaseFacade).searchKnowledgeBase(eq(knowledgeBaseId), eq(query), any());
    }

    @Test
    void testCreateKnowledgeBase() {
        Long workspaceId = 1L;
        long environmentId = 0L;
        KnowledgeBase mockKnowledgeBase = createMockKnowledgeBase(1L, "New KnowledgeBase");

        when(workspaceKnowledgeBaseFacade.createWorkspaceKnowledgeBase(
            any(KnowledgeBase.class), eq(workspaceId), eq(environmentId)))
                .thenReturn(mockKnowledgeBase);

        this.graphQlTester
            .document("""
                mutation {
                    createKnowledgeBase(
                        knowledgeBase: {name: "New KnowledgeBase"},
                        environmentId: "0",
                        workspaceId: "1"
                    ) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("createKnowledgeBase.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("createKnowledgeBase.name")
            .entity(String.class)
            .isEqualTo("New KnowledgeBase");

        verify(workspaceKnowledgeBaseFacade).createWorkspaceKnowledgeBase(
            any(KnowledgeBase.class), eq(workspaceId), eq(environmentId));
    }

    @Test
    void testUpdateKnowledgeBase() {
        Long knowledgeBaseId = 1L;
        KnowledgeBase mockKnowledgeBase = createMockKnowledgeBase(knowledgeBaseId, "Updated KnowledgeBase");

        when(knowledgeBaseService.updateKnowledgeBase(eq(knowledgeBaseId), any(KnowledgeBase.class)))
            .thenReturn(mockKnowledgeBase);

        this.graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBase(id: "1", knowledgeBase: {name: "Updated KnowledgeBase"}) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBase.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("updateKnowledgeBase.name")
            .entity(String.class)
            .isEqualTo("Updated KnowledgeBase");

        verify(knowledgeBaseService).updateKnowledgeBase(eq(knowledgeBaseId), any(KnowledgeBase.class));
    }

    @Test
    void testDeleteKnowledgeBase() {
        Long knowledgeBaseId = 1L;

        this.graphQlTester
            .document("""
                mutation {
                    deleteKnowledgeBase(id: "1")
                }
                """)
            .execute()
            .path("deleteKnowledgeBase")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workspaceKnowledgeBaseFacade).deleteWorkspaceKnowledgeBase(knowledgeBaseId);
    }

    @Test
    void testKnowledgeBaseDocuments() {
        Long knowledgeBaseId = 1L;
        KnowledgeBase mockKnowledgeBase = createMockKnowledgeBase(knowledgeBaseId, "Test KnowledgeBase");

        List<KnowledgeBaseDocument> mockDocuments = List.of(
            createMockDocument(1L, "Document 1"),
            createMockDocument(2L, "Document 2"));

        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(mockKnowledgeBase);
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBaseId)).thenReturn(mockDocuments);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBase(id: "1") {
                        id
                        documents {
                            id
                            name
                        }
                    }
                }
                """)
            .execute()
            .path("knowledgeBase.documents")
            .entityList(Object.class)
            .hasSize(2);

        verify(knowledgeBaseDocumentService).getKnowledgeBaseDocuments(knowledgeBaseId);
    }

    private KnowledgeBase createMockKnowledgeBase(Long id, String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setId(id);
        knowledgeBase.setName(name);
        knowledgeBase.setVersion(1);

        return knowledgeBase;
    }

    private KnowledgeBaseDocument createMockDocument(Long id, String name) {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setId(id);
        document.setName(name);
        document.setDocument(new FileEntry(name + ".txt", "file://test/" + name + ".txt"));
        document.setStatus(KnowledgeBaseDocument.STATUS_READY);
        document.setVersion(1);

        return document;
    }

    private KnowledgeBaseDocumentChunk createMockChunk(Long id, String content) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setId(id);
        chunk.setVectorStoreId("vector-store-id-" + id);
        chunk.setTextContent(content);
        chunk.setVersion(1);

        return chunk;
    }

    @Nested
    @Import(MethodSecurityEnforcement.MethodSecurityConfiguration.class)
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";
        private static final long KNOWLEDGE_BASE_ID = 11L;

        @Autowired
        private KnowledgeBaseGraphQlController knowledgeBaseGraphQlController;

        @MockitoBean
        private PermissionService permissionService;

        @BeforeEach
        void beforeEach() {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("member", null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            SecurityContextHolder.setContext(securityContext);

            when(knowledgeBaseService.getKnowledgeBase(any())).thenThrow(new IllegalStateException(BODY_REACHED));
            when(knowledgeBaseFacade.searchKnowledgeBase(any(), any(), any()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(knowledgeBaseService.updateKnowledgeBase(any(), any()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @ParameterizedTest(name = "{0} granted={1}")
        @MethodSource("guardCases")
        void testGuardDecidesOnTheExpectedPermissionCheck(GuardCase guardCase, boolean granted) {
            Function<PermissionService, Boolean> expectedCheck = guardCase.expectedCheck();

            when(expectedCheck.apply(permissionService)).thenReturn(granted);

            Method method = findMethod(guardCase.methodName());
            Object[] arguments = toArguments(method, guardCase.argumentsByName());

            if (granted) {
                assertThatThrownBy(() -> invoke(method, knowledgeBaseGraphQlController, arguments))
                    .as("%s must reach its body when its permission check grants", guardCase)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BODY_REACHED);
            } else {
                assertThatThrownBy(() -> invoke(method, knowledgeBaseGraphQlController, arguments))
                    .as("%s must be denied when its permission check refuses", guardCase)
                    .isInstanceOf(AccessDeniedException.class);
            }

            expectedCheck.apply(verify(permissionService));
            verifyNoMoreInteractions(permissionService);
        }

        @Test
        void testEveryGuardedMethodHasAnEvaluatedCase() {
            Set<String> guardedMethodNames = Arrays.stream(KnowledgeBaseGraphQlController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .map(Method::getName)
                .collect(Collectors.toSet());

            Set<String> evaluatedMethodNames = guardCaseStream()
                .map(GuardCase::methodName)
                .collect(Collectors.toSet());

            assertThat(evaluatedMethodNames).isEqualTo(guardedMethodNames);
        }

        static Stream<Arguments> guardCases() {
            return guardCaseStream()
                .flatMap(guardCase -> Stream.of(Arguments.of(guardCase, false), Arguments.of(guardCase, true)));
        }

        private static Stream<GuardCase> guardCaseStream() {
            return Stream.of(
                new GuardCase("knowledgeBase", Map.of("id", KNOWLEDGE_BASE_ID),
                    knowledgeBaseCheck("KNOWLEDGE_BASE_VIEW")),
                new GuardCase(
                    "searchKnowledgeBase",
                    Map.of("id", KNOWLEDGE_BASE_ID, "query", "invoices"), knowledgeBaseCheck("KNOWLEDGE_BASE_VIEW")),
                new GuardCase(
                    "updateKnowledgeBase", Map.of("id", KNOWLEDGE_BASE_ID), knowledgeBaseCheck("KNOWLEDGE_BASE_EDIT")));
        }

        private static Function<PermissionService, Boolean> knowledgeBaseCheck(String scope) {
            return permissionService -> permissionService.hasResourceScope(KNOWLEDGE_BASE_ID, "KnowledgeBase", scope);
        }

        private static Object invoke(Method method, Object target, Object[] arguments) throws Throwable {
            try {
                return method.invoke(target, arguments);
            } catch (InvocationTargetException invocationTargetException) {
                throw invocationTargetException.getCause();
            }
        }

        private static Method findMethod(String methodName) {
            List<Method> methods = Arrays.stream(KnowledgeBaseGraphQlController.class.getDeclaredMethods())
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
            String methodName, Map<String, Object> argumentsByName,
            Function<PermissionService, Boolean> expectedCheck) {

            @Override
            public String toString() {
                return methodName;
            }
        }

        @EnableMethodSecurity
        static class MethodSecurityConfiguration {

            @Bean
            static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
                @Lazy PermissionService permissionService) {

                AutomationMethodSecurityExpressionHandler expressionHandler =
                    new AutomationMethodSecurityExpressionHandler(permissionService);

                expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

                return expressionHandler;
            }
        }
    }
}
