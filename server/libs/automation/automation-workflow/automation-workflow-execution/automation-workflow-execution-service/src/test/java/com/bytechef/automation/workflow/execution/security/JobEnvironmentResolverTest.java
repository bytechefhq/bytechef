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

package com.bytechef.automation.workflow.execution.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Without this resolver a job check answered from the member's roles across every environment, so an editor in
 * Development could restart or stop a Production job.
 *
 * @author Ivica Cardic
 */
class JobEnvironmentResolverTest {

    private static final long JOB_ID = 3L;
    private static final long PROJECT_DEPLOYMENT_ID = 11L;

    private final PrincipalJobService principalJobService = mock(PrincipalJobService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final JobEnvironmentResolver resolver = new JobEnvironmentResolver(
        principalJobService, projectDeploymentService);

    @Test
    void testReportsTheEnvironmentOfTheDeploymentThatRanTheJob() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(PROJECT_DEPLOYMENT_ID));
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(projectDeployment));

        assertThat(resolver.fetchEnvironment(JOB_ID)).contains(Environment.PRODUCTION);
    }

    @Test
    void testAJobWithoutADeploymentHasNoEnvironment() {
        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.AUTOMATION)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(JOB_ID)).isEmpty();
    }
}
