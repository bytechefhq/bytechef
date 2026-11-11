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

package com.bytechef.automation.configuration.web.graphql;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.automation.configuration.dto.AutomationGettingStartedDTO;
import com.bytechef.automation.configuration.dto.AutomationGettingStartedRunDTO;
import com.bytechef.automation.configuration.facade.AutomationGettingStartedFacade;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationConfigurationGraphQlTestConfiguration.class,
    AutomationGettingStartedGraphQlController.class
})
@GraphQlTest(
    controllers = AutomationGettingStartedGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.inspection.enabled=false",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@AutomationConfigurationGraphQlConfigurationSharedMocks
class AutomationGettingStartedGraphQlControllerIntTest {

    private static final Instant RUN_DATE = Instant.parse("2026-11-11T10:15:30Z");

    @Autowired
    private AutomationGettingStartedFacade automationGettingStartedFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testAutomationGettingStarted() {
        when(automationGettingStartedFacade.getAutomationGettingStarted(5L))
            .thenReturn(new AutomationGettingStartedDTO(
                false, true, new AutomationGettingStartedRunDTO(1L, Job.Status.COMPLETED, RUN_DATE, null, 11L, true),
                true, new AutomationGettingStartedRunDTO(2L, Job.Status.FAILED, RUN_DATE, "boom", 12L, null), false,
                false, true, true, true));

        graphQlTester
            .document("""
                query {
                    automationGettingStarted(workspaceId: 5) {
                        completed
                        deployed
                        firstRun {
                            date
                            errorMessage
                            jobId
                            projectDeploymentEnabled
                            projectDeploymentId
                            status
                        }
                        httpClientAdded
                        lastFailedRun {
                            date
                            errorMessage
                            jobId
                            projectDeploymentEnabled
                            projectDeploymentId
                            status
                        }
                        live
                        loggerAdded
                        projectCreated
                        published
                        workflowCreated
                    }
                }
                """)
            .execute()
            .path("automationGettingStarted.completed")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("automationGettingStarted.httpClientAdded")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationGettingStarted.loggerAdded")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("automationGettingStarted.deployed")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationGettingStarted.live")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("automationGettingStarted.projectCreated")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationGettingStarted.published")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationGettingStarted.workflowCreated")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationGettingStarted.firstRun.status")
            .entity(String.class)
            .isEqualTo("COMPLETED")
            .path("automationGettingStarted.firstRun.date")
            .entity(String.class)
            .isEqualTo("2026-11-11T10:15:30Z")
            .path("automationGettingStarted.firstRun.errorMessage")
            .valueIsNull()
            .path("automationGettingStarted.firstRun.jobId")
            .entity(Long.class)
            .isEqualTo(1L)
            .path("automationGettingStarted.firstRun.projectDeploymentId")
            .entity(Long.class)
            .isEqualTo(11L)
            .path("automationGettingStarted.firstRun.projectDeploymentEnabled")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationGettingStarted.lastFailedRun.status")
            .entity(String.class)
            .isEqualTo("FAILED")
            .path("automationGettingStarted.lastFailedRun.errorMessage")
            .entity(String.class)
            .isEqualTo("boom")
            .path("automationGettingStarted.lastFailedRun.projectDeploymentEnabled")
            .valueIsNull();
    }

    @Test
    void testAutomationGettingStartedWithoutRunsReturnsNull() {
        when(automationGettingStartedFacade.getAutomationGettingStarted(5L))
            .thenReturn(
                new AutomationGettingStartedDTO(false, false, null, false, null, false, false, false, false, false));

        graphQlTester
            .document("""
                query {
                    automationGettingStarted(workspaceId: 5) {
                        firstRun {
                            jobId
                        }
                        lastFailedRun {
                            jobId
                        }
                    }
                }
                """)
            .execute()
            .path("automationGettingStarted.firstRun")
            .valueIsNull()
            .path("automationGettingStarted.lastFailedRun")
            .valueIsNull();
    }

    @Test
    void testAutomationGettingStartedCompleted() {
        when(automationGettingStartedFacade.isAutomationGettingStartedCompleted(5L)).thenReturn(true);

        graphQlTester
            .document("""
                query {
                    automationGettingStartedCompleted(workspaceId: 5)
                }
                """)
            .execute()
            .path("automationGettingStartedCompleted")
            .entity(Boolean.class)
            .isEqualTo(true);
    }

    @Test
    void testDismissAutomationGettingStarted() {
        graphQlTester
            .document("""
                mutation {
                    dismissAutomationGettingStarted(workspaceId: 5)
                }
                """)
            .execute()
            .path("dismissAutomationGettingStarted")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationGettingStartedFacade).dismissAutomationGettingStarted(5L);
    }
}
