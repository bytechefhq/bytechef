/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectConfigurationAdminFacade;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedConfigurationGraphQlTestConfiguration.class,
    AutomationWorkflowProjectConfigurationGraphQlController.class
})
@GraphQlTest(
    controllers = AutomationWorkflowProjectConfigurationGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
@MockitoBean(types = AutomationWorkflowProjectConfigurationAdminFacade.class)
class AutomationWorkflowProjectConfigurationGraphQlControllerIntTest {

    @Autowired
    private AutomationWorkflowProjectConfigurationAdminFacade automationWorkflowProjectConfigurationAdminFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testAutomationWorkflowProjectConfigurationsReturnsWorkflowsWithTheirSwitches() {
        AutomationWorkflowProjectConfigurationDTO configuration = new AutomationWorkflowProjectConfigurationDTO(
            5L, 7L, 0L, 2, true, false,
            List.of(
                new AutomationWorkflowProjectConfigurationDTO.Workflow("uuid-one", "workflow-id-one", "One", true,
                    null),
                new AutomationWorkflowProjectConfigurationDTO.Workflow("uuid-two", "workflow-id-two", "Two", false,
                    null)),
            Instant.parse("2026-10-28T10:00:00Z"), null);

        when(automationWorkflowProjectConfigurationAdminFacade.getConfigurations(7L))
            .thenReturn(List.of(configuration));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurations(projectId: "7") {
                        id
                        projectId
                        environmentId
                        projectVersion
                        enabled
                        automationHubVisible
                        lastModifiedDate
                        workflows {
                            workflowUuid
                            workflowId
                            label
                            enabled
                        }
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurations[0].id")
            .entity(String.class)
            .isEqualTo("5")
            .path("automationWorkflowProjectConfigurations[0].projectVersion")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("automationWorkflowProjectConfigurations[0].automationHubVisible")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("automationWorkflowProjectConfigurations[*].workflows[*].enabled")
            .entityList(Boolean.class)
            .containsExactly(true, false)
            .path("automationWorkflowProjectConfigurations[0].workflows[1].label")
            .entity(String.class)
            .isEqualTo("Two")
            .path("automationWorkflowProjectConfigurations[0].workflows[*].workflowId")
            .entityList(String.class)
            .containsExactly("workflow-id-one", "workflow-id-two");
    }

    @Test
    void testAutomationWorkflowProjectConfigurationVersionImpactReturnsTheWorkflowsItWouldPause() {
        when(automationWorkflowProjectConfigurationAdminFacade.getVersionImpact(5L, 4))
            .thenReturn(
                new AutomationWorkflowProjectConfigurationVersionImpactDTO(
                    List.of(
                        new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                            "uuid-one", "Notify Slack", List.of("channel"), List.of("jira"), 12))));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectConfigurationVersionImpact(id: "5", projectVersion: 4) {
                        workflows {
                            workflowUuid
                            label
                            newRequiredInputs
                            newRequiredConnections
                            affectedConnectedUserCount
                        }
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectConfigurationVersionImpact.workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo("uuid-one")
            .path("automationWorkflowProjectConfigurationVersionImpact.workflows[0].label")
            .entity(String.class)
            .isEqualTo("Notify Slack")
            .path("automationWorkflowProjectConfigurationVersionImpact.workflows[0].newRequiredInputs")
            .entityList(String.class)
            .containsExactly("channel")
            .path("automationWorkflowProjectConfigurationVersionImpact.workflows[0].newRequiredConnections")
            .entityList(String.class)
            .containsExactly("jira")
            .path("automationWorkflowProjectConfigurationVersionImpact.workflows[0].affectedConnectedUserCount")
            .entity(Integer.class)
            .isEqualTo(12);
    }

    @Test
    void testCreateAutomationWorkflowProjectConfigurationDelegatesAndReturnsId() {
        when(automationWorkflowProjectConfigurationAdminFacade.createConfiguration(7L, 0L, 2, true, false))
            .thenReturn(11L);

        this.graphQlTester
            .document("""
                mutation {
                    createAutomationWorkflowProjectConfiguration(
                        projectId: "7", environmentId: "0", projectVersion: 2, enabled: true,
                        automationHubVisible: false)
                }
                """)
            .execute()
            .path("createAutomationWorkflowProjectConfiguration")
            .entity(String.class)
            .isEqualTo("11");
    }

    @Test
    void testEnableAutomationWorkflowProjectConfigurationWorkflowDelegates() {
        this.graphQlTester
            .document("""
                mutation {
                    enableAutomationWorkflowProjectConfigurationWorkflow(id: "5", workflowUuid: "uuid", enable: false)
                }
                """)
            .execute()
            .path("enableAutomationWorkflowProjectConfigurationWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectConfigurationAdminFacade)
            .enableConfigurationWorkflow(5L, "uuid", false);
    }

    @Test
    void testDeleteAutomationWorkflowProjectConfigurationDelegates() {
        this.graphQlTester
            .document("""
                mutation {
                    deleteAutomationWorkflowProjectConfiguration(id: "5")
                }
                """)
            .execute()
            .path("deleteAutomationWorkflowProjectConfiguration")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectConfigurationAdminFacade).deleteConfiguration(5L);
    }
}
