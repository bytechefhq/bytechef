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

package com.bytechef.platform.component.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class EditorLogFileStorageTest {

    private static final long JOB_ID = 7L;
    private static final long TASK_EXECUTION_ID = 8L;
    private static final String EDITOR_DIR = "editor/logs";
    private static final String EDITOR_JOB_DIR = "editor/logs/7";

    @Mock
    private FileStorageService fileStorageService;

    private EditorLogFileStorage editorLogFileStorage;

    @BeforeEach
    void beforeEach() {
        editorLogFileStorage = new EditorLogFileStorageImpl(fileStorageService);
    }

    @Test
    void testEntriesAreWrittenUnderTheEditorDirectory() {
        when(fileStorageService.fileExists(EDITOR_JOB_DIR, "70.jsonl")).thenReturn(false);
        when(fileStorageService.getFileEntries(EDITOR_JOB_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(EDITOR_DIR, "7.jsonl")).thenReturn(false);

        editorLogFileStorage.storeLogEntries(JOB_ID, 70L, List.of(logEntry("editor run")));

        editorLogFileStorage.logsExist(JOB_ID);

        verify(fileStorageService).storeFileContent(eq(EDITOR_JOB_DIR), eq("70.jsonl"), any(byte[].class), eq(false));
    }

    @Test
    void testLegacyEditorJobFilesAreStillRead() {
        FileEntry legacyFile = new FileEntry("7.jsonl", "file://test/editor/7.jsonl");

        when(fileStorageService.getFileEntries(EDITOR_JOB_DIR + "/")).thenReturn(Set.of());
        when(fileStorageService.fileExists(EDITOR_DIR, "7.jsonl")).thenReturn(true);
        when(fileStorageService.getFileEntry(EDITOR_DIR, "7.jsonl")).thenReturn(legacyFile);
        when(fileStorageService.readFileToBytes(EDITOR_DIR, legacyFile))
            .thenReturn((JsonUtils.write(logEntry("from before")) + "\n").getBytes(StandardCharsets.UTF_8));

        List<LogEntry> logEntries = editorLogFileStorage.readLogEntriesByJobId(JOB_ID);

        assertEquals(List.of("from before"), logEntries.stream()
            .map(LogEntry::message)
            .toList());
    }

    private static LogEntry logEntry(String message) {
        return LogEntry.builder()
            .timestamp(Instant.now())
            .level(LogEntry.Level.DEBUG)
            .componentName("logger")
            .taskExecutionId(70L)
            .message(message)
            .build();
    }

    @Nested
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";

        private final PermissionService permissionService = mock(PermissionService.class);
        private final EditorLogFileStorage securedEditorLogFileStorage = secure(
            new EditorLogFileStorageImpl(
                mock(FileStorageService.class, invocation -> {
                    throw new IllegalStateException(BODY_REACHED);
                })));

        @BeforeEach
        void beforeEach() {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

            SecurityContextHolder.setContext(securityContext);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @ParameterizedTest
        @MethodSource("guardedOperations")
        void testGuardDeniesWhenTheTestJobEditScopeIsRefused(GuardedOperation guardedOperation) {
            assertThatThrownBy(() -> guardedOperation.invoke(securedEditorLogFileStorage))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionService).hasResourceScope(JOB_ID, "TestJob", "WORKFLOW_EDIT");
        }

        @ParameterizedTest
        @MethodSource("guardedOperations")
        void testGuardAllowsWhenTheTestJobEditScopeIsGranted(GuardedOperation guardedOperation) {
            when(permissionService.hasResourceScope(JOB_ID, "TestJob", "WORKFLOW_EDIT")).thenReturn(true);

            assertThatThrownBy(() -> guardedOperation.invoke(securedEditorLogFileStorage))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        }

        @Test
        void testEveryGuardedMethodHasAnEvaluatedCase() {
            Set<String> guardedMethodNames = Arrays.stream(EditorLogFileStorageImpl.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .map(Method::getName)
                .collect(Collectors.toSet());

            Set<String> coveredMethodNames = guardedOperations()
                .map(Named::getName)
                .map(name -> name.split(" ")[0])
                .collect(Collectors.toSet());

            assertThat(coveredMethodNames).isEqualTo(guardedMethodNames);
        }

        static Stream<Named<GuardedOperation>> guardedOperations() {
            return Stream.of(
                Named.of(
                    "deleteLogEntries",
                    (GuardedOperation) guardedStorage -> guardedStorage.deleteLogEntries(JOB_ID)),
                Named.of("logsExist", (GuardedOperation) guardedStorage -> guardedStorage.logsExist(JOB_ID)),
                Named.of(
                    "readLogEntries",
                    (GuardedOperation) guardedStorage -> guardedStorage.readLogEntries(JOB_ID, TASK_EXECUTION_ID)),
                Named.of(
                    "readLogEntriesByJobId",
                    (GuardedOperation) guardedStorage -> guardedStorage.readLogEntriesByJobId(JOB_ID)));
        }

        private EditorLogFileStorage secure(EditorLogFileStorage targetEditorLogFileStorage) {
            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(permissionService);

            expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));
            expressionHandler.setRoleHierarchy(
                RoleHierarchyImpl.withDefaultRolePrefix()
                    .role("ADMIN")
                    .implies("USER")
                    .build());

            PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager =
                new PreAuthorizeAuthorizationManager();

            preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

            ProxyFactory proxyFactory = new ProxyFactory(targetEditorLogFileStorage);

            proxyFactory.addAdvisor(
                AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

            return (EditorLogFileStorage) proxyFactory.getProxy();
        }

        @FunctionalInterface
        interface GuardedOperation {

            void invoke(EditorLogFileStorage guardedStorage);
        }
    }
}
