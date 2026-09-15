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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlConfigurationSharedMocks;
import com.bytechef.automation.knowledgebase.web.graphql.config.AutomationKnowledgeBaseGraphQlTestConfiguration;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
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
 * Integration tests for {@link KnowledgeBaseDocumentChunkGraphQlController}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationKnowledgeBaseGraphQlTestConfiguration.class,
    KnowledgeBaseDocumentChunkGraphQlController.class
})
@GraphQlTest(
    controllers = KnowledgeBaseDocumentChunkGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.ai.knowledge-base.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
@AutomationKnowledgeBaseGraphQlConfigurationSharedMocks
class KnowledgeBaseDocumentChunkGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;

    @Test
    void testUpdateKnowledgeBaseDocumentChunk() {
        Long chunkId = 1L;
        String newContent = "Updated content";

        KnowledgeBaseDocumentChunk mockChunk = createMockChunk(chunkId, newContent);

        when(knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(eq(chunkId), eq(newContent)))
            .thenReturn(mockChunk);

        this.graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBaseDocumentChunk(
                        id: "1",
                        knowledgeBaseDocumentChunk: {content: "Updated content"}
                    ) {
                        id
                        content
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBaseDocumentChunk.id")
            .entity(String.class)
            .isEqualTo("1")
            .path("updateKnowledgeBaseDocumentChunk.content")
            .entity(String.class)
            .isEqualTo("Updated content");

        verify(knowledgeBaseDocumentChunkFacade).updateKnowledgeBaseDocumentChunk(eq(chunkId), eq(newContent));
    }

    @Test
    void testDeleteKnowledgeBaseDocumentChunk() {
        Long chunkId = 1L;

        this.graphQlTester
            .document("""
                mutation {
                    deleteKnowledgeBaseDocumentChunk(id: "1")
                }
                """)
            .execute()
            .path("deleteKnowledgeBaseDocumentChunk")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(knowledgeBaseDocumentChunkFacade).deleteKnowledgeBaseDocumentChunk(chunkId);
    }

    @Test
    void testChunkContentSchemaMapping() {
        Long chunkId = 1L;
        String content = "Test chunk content";

        KnowledgeBaseDocumentChunk mockChunk = createMockChunk(chunkId, content);

        when(knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(eq(chunkId), eq(content)))
            .thenReturn(mockChunk);

        this.graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBaseDocumentChunk(
                        id: "1",
                        knowledgeBaseDocumentChunk: {content: "Test chunk content"}
                    ) {
                        id
                        content
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBaseDocumentChunk.content")
            .entity(String.class)
            .isEqualTo("Test chunk content");
    }

    @Test
    void testChunkMetadataSchemaMapping() {
        Long chunkId = 1L;
        String content = "Test content";

        KnowledgeBaseDocumentChunk mockChunk = createMockChunk(chunkId, content);

        mockChunk.setMetadata(Map.of("key1", "value1", "key2", "value2"));

        when(knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(eq(chunkId), eq(content)))
            .thenReturn(mockChunk);

        this.graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBaseDocumentChunk(
                        id: "1",
                        knowledgeBaseDocumentChunk: {content: "Test content"}
                    ) {
                        id
                        metadata
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBaseDocumentChunk.id")
            .entity(String.class)
            .isEqualTo("1");
    }

    @Test
    void testChunkScoreSchemaMapping() {
        Long chunkId = 1L;
        String content = "Test content";

        KnowledgeBaseDocumentChunk mockChunk = createMockChunk(chunkId, content);

        mockChunk.setScore(0.95f);

        when(knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(eq(chunkId), eq(content)))
            .thenReturn(mockChunk);

        this.graphQlTester
            .document("""
                mutation {
                    updateKnowledgeBaseDocumentChunk(
                        id: "1",
                        knowledgeBaseDocumentChunk: {content: "Test content"}
                    ) {
                        id
                        score
                    }
                }
                """)
            .execute()
            .path("updateKnowledgeBaseDocumentChunk.score")
            .entity(Float.class)
            .isEqualTo(0.95f);
    }

    private KnowledgeBaseDocumentChunk createMockChunk(Long id, String content) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setId(id);
        chunk.setKnowledgeBaseDocumentId(1L);
        chunk.setVectorStoreId("vector-store-id-" + id);
        chunk.setTextContent(content);
        chunk.setVersion(1);

        return chunk;
    }

    @Nested
    @Import(MethodSecurityEnforcement.MethodSecurityConfiguration.class)
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";
        private static final long CHUNK_ID = 13L;

        @Autowired
        private KnowledgeBaseDocumentChunkGraphQlController knowledgeBaseDocumentChunkGraphQlController;

        @MockitoBean
        private PermissionService permissionService;

        @BeforeEach
        void beforeEach() {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("member", null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            SecurityContextHolder.setContext(securityContext);

            when(knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(anyLong(), any()))
                .thenThrow(new IllegalStateException(BODY_REACHED));

            doThrow(new IllegalStateException(BODY_REACHED)).when(knowledgeBaseDocumentChunkFacade)
                .deleteKnowledgeBaseDocumentChunk(anyLong());
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
                assertThatThrownBy(() -> invoke(method, knowledgeBaseDocumentChunkGraphQlController, arguments))
                    .as("%s must reach its body when its permission check grants", guardCase)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BODY_REACHED);
            } else {
                assertThatThrownBy(() -> invoke(method, knowledgeBaseDocumentChunkGraphQlController, arguments))
                    .as("%s must be denied when its permission check refuses", guardCase)
                    .isInstanceOf(AccessDeniedException.class);
            }

            expectedCheck.apply(verify(permissionService));
            verifyNoMoreInteractions(permissionService);
        }

        @Test
        void testEveryGuardedMethodHasAnEvaluatedCase() {
            Set<String> guardedMethodNames =
                Arrays.stream(KnowledgeBaseDocumentChunkGraphQlController.class.getDeclaredMethods())
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
                new GuardCase(
                    "updateKnowledgeBaseDocumentChunk",
                    Map.of(
                        "id", CHUNK_ID, "knowledgeBaseDocumentChunk",
                        new KnowledgeBaseDocumentChunkGraphQlController.KnowledgeBaseDocumentChunkInput(
                            "updated content")),
                    chunkCheck()),
                new GuardCase("deleteKnowledgeBaseDocumentChunk", Map.of("id", CHUNK_ID), chunkCheck()));
        }

        private static Function<PermissionService, Boolean> chunkCheck() {
            return permissionService -> permissionService.hasResourceScope(
                CHUNK_ID, "KnowledgeBaseDocumentChunk", "KNOWLEDGE_BASE_EDIT");
        }

        private static Object invoke(Method method, Object target, Object[] arguments) throws Throwable {
            try {
                return method.invoke(target, arguments);
            } catch (InvocationTargetException invocationTargetException) {
                throw invocationTargetException.getCause();
            }
        }

        private static Method findMethod(String methodName) {
            List<Method> methods = Arrays.stream(KnowledgeBaseDocumentChunkGraphQlController.class.getDeclaredMethods())
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
