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

package com.bytechef.platform.workflow.worker.task.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AbstractTaskHandlerTest {

    private final ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);

    private final AbstractTaskHandler taskHandler = new AbstractTaskHandler(
        "aiAgentUtils", 1, "createAiSkill", actionDefinitionFacade) {};

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testHandleRunsAsSystemWhenNoUserIsAuthenticated() throws Exception {
        stubExecutePerformReturningCurrentLogin();

        assertThat(taskHandler.handle(taskExecution())).isEqualTo(SecurityUtils.SYSTEM_LOGIN);
        assertThat(SecurityContextHolder.getContext()
            .getAuthentication()).isNull();
    }

    @Test
    void testHandleKeepsTheAuthenticatedUser() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        stubExecutePerformReturningCurrentLogin();

        assertThat(taskHandler.handle(taskExecution())).isEqualTo("alice");
    }

    private void stubExecutePerformReturningCurrentLogin() {
        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), anyMap(), anyMap(), anyMap(),
                any(), any(), anyBoolean(), any(), any(), any()))
                    .thenAnswer(invocation -> SecurityUtils.fetchCurrentUserLogin()
                        .orElse(null));
    }

    private static TaskExecution taskExecution() {
        return TaskExecution.builder()
            .id(2L)
            .jobId(1L)
            .workflowTask(new WorkflowTask(Map.of("name", "createAiSkill", "type", "aiAgentUtils/v1/createAiSkill")))
            .build();
    }
}
