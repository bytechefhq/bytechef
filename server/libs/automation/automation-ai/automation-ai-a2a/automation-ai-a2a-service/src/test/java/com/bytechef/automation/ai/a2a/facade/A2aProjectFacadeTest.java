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

package com.bytechef.automation.ai.a2a.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class A2aProjectFacadeTest {

    private final A2aProjectService a2aProjectService = mock(A2aProjectService.class);
    private final A2aProjectWorkflowService a2aProjectWorkflowService = mock(A2aProjectWorkflowService.class);
    private final A2aServerService a2aServerService = mock(A2aServerService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);

    @Test
    void testCreateA2aProjectUsesServerEnvironment() {
        A2aServer a2aServer = new A2aServer("prod-agent", null, PlatformType.AUTOMATION, Environment.PRODUCTION);

        a2aServer.setId(7L);

        when(a2aServerService.getA2aServer(7L)).thenReturn(a2aServer);

        ProjectDeployment savedProjectDeployment = new ProjectDeployment();

        savedProjectDeployment.setId(100L);

        when(projectDeploymentService.create(any())).thenReturn(savedProjectDeployment);
        when(a2aProjectService.create(100L, 7L)).thenReturn(new A2aProject());

        A2aProjectFacadeImpl a2aProjectFacade = new A2aProjectFacadeImpl(
            a2aProjectService, a2aProjectWorkflowService, a2aServerService, projectDeploymentService,
            projectDeploymentWorkflowService);

        a2aProjectFacade.createA2aProject(7L, 42L, 3, List.of());

        ArgumentCaptor<ProjectDeployment> captor = ArgumentCaptor.forClass(ProjectDeployment.class);

        verify(projectDeploymentService).create(captor.capture());

        assertThat(captor.getValue()
            .getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
