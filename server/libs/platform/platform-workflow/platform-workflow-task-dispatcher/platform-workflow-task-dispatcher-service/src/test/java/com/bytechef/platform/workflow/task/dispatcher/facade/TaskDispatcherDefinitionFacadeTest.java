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

package com.bytechef.platform.workflow.task.dispatcher.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class TaskDispatcherDefinitionFacadeTest {

    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService =
        mock(TaskDispatcherDefinitionService.class);

    private TaskDispatcherDefinitionFacade taskDispatcherDefinitionFacade;

    @BeforeEach
    void beforeEach() {
        ProxyFactory proxyFactory = new ProxyFactory(
            new TaskDispatcherDefinitionFacadeImpl(taskDispatcherDefinitionService));

        proxyFactory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());

        taskDispatcherDefinitionFacade = (TaskDispatcherDefinitionFacade) proxyFactory.getProxy();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testReadsRequireAuthentication() {
        assertThatThrownBy(() -> taskDispatcherDefinitionFacade.getTaskDispatcherDefinitions())
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> taskDispatcherDefinitionFacade.getTaskDispatcherDefinition("loop", 1))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> taskDispatcherDefinitionFacade.executeOutput("loop", 1, Map.of()))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(taskDispatcherDefinitionService);
    }

    @Test
    void testAuthenticatedCallerReachesTheService() {
        TaskDispatcherDefinition taskDispatcherDefinition = mock(TaskDispatcherDefinition.class);

        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("user", "n/a", "ROLE_USER"));

        when(taskDispatcherDefinitionService.getTaskDispatcherDefinitions())
            .thenReturn(List.of(taskDispatcherDefinition));

        assertThat(taskDispatcherDefinitionFacade.getTaskDispatcherDefinitions()).containsExactly(
            taskDispatcherDefinition);

        verify(taskDispatcherDefinitionService, never()).executeOutput("loop", 1, Map.of());
    }
}
