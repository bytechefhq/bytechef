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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlConfigurationSharedMocks;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlTestConfiguration;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
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
 * Integration tests for {@link KnowledgeBaseDocumentGraphQlController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationKnowledgeBaseGraphQlTestConfiguration.class,
    KnowledgeBaseDocumentGraphQlController.class
})
@GraphQlTest(
    controllers = KnowledgeBaseDocumentGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.ai.knowledge-base.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
@AutomationKnowledgeBaseGraphQlConfigurationSharedMocks
class KnowledgeBaseDocumentGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;

    @Autowired
    private KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;

    @Autowired
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Test
    void testGetKnowledgeBaseDocument() {
        Long documentId = 1L;
        KnowledgeBaseDocument mockDocument = createMockDocument(documentId, "Test Document");

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(mockDocument);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocument(id: "1") {
                        id
                        name
                        status
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocument.id")
            .entity(String.class)
            .isEqualTo("1");
    }

    @Test
    void testDocumentChunksFieldLoadsWithoutContent() {
        Long documentId = 1L;
        KnowledgeBaseDocument mockDocument = createMockDocument(documentId, "Test Document");

        List<KnowledgeBaseDocumentChunk> mockChunks = List.of(
            createMockChunk(1L),
            createMockChunk(2L));

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(mockDocument);
        when(knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentIdWithoutContent(documentId))
            .thenReturn(mockChunks);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocument(id: "1") {
                        id
                        chunks {
                            id
                        }
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocument.chunks")
            .entityList(Object.class)
            .hasSize(2);

        verify(knowledgeBaseDocumentChunkFacade).getKnowledgeBaseDocumentChunksByDocumentIdWithoutContent(documentId);
    }

    @Test
    void testGetKnowledgeBaseDocumentChunks() {
        Long documentId = 1L;

        List<KnowledgeBaseDocumentChunk> mockChunks = List.of(
            createMockChunk(1L),
            createMockChunk(2L));

        when(knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(documentId))
            .thenReturn(mockChunks);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocumentChunks(id: "1") {
                        id
                        knowledgeBaseDocumentId
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocumentChunks")
            .entityList(Object.class)
            .hasSize(2);

        verify(knowledgeBaseDocumentChunkFacade).getKnowledgeBaseDocumentChunksByDocumentId(documentId);
    }

    @Test
    void testDocumentTags() {
        Long documentId = 1L;
        KnowledgeBaseDocument mockDocument = createMockDocument(documentId, "Test Document");

        mockDocument.setTagNames(List.of("Tag 1", "Tag 2"));

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(mockDocument);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocument(id: "1") {
                        id
                        tags
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocument.tags")
            .entityList(String.class)
            .hasSize(2);
    }

    @Test
    void testDocumentTagsEmpty() {
        Long documentId = 1L;
        KnowledgeBaseDocument mockDocument = createMockDocument(documentId, "Test Document");

        mockDocument.setTagNames(List.of());

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(mockDocument);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocument(id: "1") {
                        id
                        tags
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocument.tags")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void testDeleteKnowledgeBaseDocument() {
        Long documentId = 1L;

        this.graphQlTester
            .document("""
                mutation {
                    deleteKnowledgeBaseDocument(id: "1")
                }
                """)
            .execute()
            .path("deleteKnowledgeBaseDocument")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(knowledgeBaseDocumentFacade).deleteKnowledgeBaseDocument(documentId);
    }

    @Test
    void testGetKnowledgeBaseDocumentStatus() {
        Long documentId = 1L;
        DocumentStatusUpdate statusUpdate = new DocumentStatusUpdate(documentId, 2, System.currentTimeMillis(), null);

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocumentStatus(documentId)).thenReturn(statusUpdate);

        this.graphQlTester
            .document("""
                query {
                    knowledgeBaseDocumentStatus(id: "1") {
                        documentId
                        status
                    }
                }
                """)
            .execute()
            .path("knowledgeBaseDocumentStatus.status")
            .entity(Integer.class)
            .isEqualTo(2);
    }

    private KnowledgeBaseDocument createMockDocument(Long id, String name) {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setId(id);
        document.setKnowledgeBaseId(1L);
        document.setName(name);
        document.setDocument(new FileEntry(name + ".txt", "file://test/" + name + ".txt"));
        document.setStatus(KnowledgeBaseDocument.STATUS_READY);
        document.setVersion(1);

        return document;
    }

    private KnowledgeBaseDocumentChunk createMockChunk(Long id) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setId(id);
        chunk.setKnowledgeBaseDocumentId(1L);

        return chunk;
    }

    @Nested
    @Import(MethodSecurityEnforcement.MethodSecurityConfiguration.class)
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";
        private static final long DOCUMENT_ID = 12L;

        @Autowired
        private KnowledgeBaseDocumentGraphQlController knowledgeBaseDocumentGraphQlController;

        @MockitoBean
        private PermissionService permissionService;

        @BeforeEach
        void beforeEach() {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("member", null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            SecurityContextHolder.setContext(securityContext);

            when(knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(anyLong()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(anyLong()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(knowledgeBaseDocumentService.getKnowledgeBaseDocumentStatus(anyLong()))
                .thenThrow(new IllegalStateException(BODY_REACHED));

            doThrow(new IllegalStateException(BODY_REACHED)).when(knowledgeBaseDocumentFacade)
                .deleteKnowledgeBaseDocument(any());
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
                assertThatThrownBy(() -> invoke(method, knowledgeBaseDocumentGraphQlController, arguments))
                    .as("%s must reach its body when its permission check grants", guardCase)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BODY_REACHED);
            } else {
                assertThatThrownBy(() -> invoke(method, knowledgeBaseDocumentGraphQlController, arguments))
                    .as("%s must be denied when its permission check refuses", guardCase)
                    .isInstanceOf(AccessDeniedException.class);
            }

            expectedCheck.apply(verify(permissionService));
            verifyNoMoreInteractions(permissionService);
        }

        @Test
        void testEveryGuardedMethodHasAnEvaluatedCase() {
            Set<String> guardedMethodNames =
                Arrays.stream(KnowledgeBaseDocumentGraphQlController.class.getDeclaredMethods())
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
                new GuardCase("knowledgeBaseDocument", Map.of("id", DOCUMENT_ID), documentCheck("KNOWLEDGE_BASE_VIEW")),
                new GuardCase(
                    "knowledgeBaseDocumentChunks", Map.of("id", DOCUMENT_ID), documentCheck("KNOWLEDGE_BASE_VIEW")),
                new GuardCase(
                    "knowledgeBaseDocumentStatus", Map.of("id", DOCUMENT_ID), documentCheck("KNOWLEDGE_BASE_VIEW")),
                new GuardCase(
                    "deleteKnowledgeBaseDocument", Map.of("id", DOCUMENT_ID), documentCheck("KNOWLEDGE_BASE_EDIT")));
        }

        private static Function<PermissionService, Boolean> documentCheck(String scope) {
            return permissionService -> permissionService.hasResourceScope(DOCUMENT_ID, "KnowledgeBaseDocument", scope);
        }

        private static Object invoke(Method method, Object target, Object[] arguments) throws Throwable {
            try {
                return method.invoke(target, arguments);
            } catch (InvocationTargetException invocationTargetException) {
                throw invocationTargetException.getCause();
            }
        }

        private static Method findMethod(String methodName) {
            List<Method> methods = Arrays.stream(KnowledgeBaseDocumentGraphQlController.class.getDeclaredMethods())
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
