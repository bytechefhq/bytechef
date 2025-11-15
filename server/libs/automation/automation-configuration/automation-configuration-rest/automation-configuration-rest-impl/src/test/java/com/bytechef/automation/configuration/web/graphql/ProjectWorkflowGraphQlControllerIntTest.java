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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.dto.SharedWorkflowDTO;
import com.bytechef.automation.configuration.dto.WorkflowTemplateDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationConfigurationGraphQlTestConfiguration.class,
    ProjectWorkflowGraphQlController.class,
})
@GraphQlTest(
    controllers = ProjectWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/",
    })
public class ProjectWorkflowGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Test
    void testDeleteSharedWorkflow() {
        // Given
        doNothing().when(projectWorkflowFacade)
            .deleteSharedWorkflow(anyString());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    deleteSharedWorkflow(workflowId: \"wf-1\")
                }
                """)
            .execute()
            .path("deleteSharedWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);
    }

    @Test
    void testExportSharedWorkflow() {
        // Given
        doNothing().when(projectWorkflowFacade)
            .exportSharedWorkflow(anyString(), anyString());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    exportSharedWorkflow(workflowId: \"wf-1\", description: \"some desc\")
                }
                """)
            .execute()
            .path("exportSharedWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);
    }

    @Test
    void testGetSharedWorkflow() {
        // Given: mock facade to return a SharedWorkflowDTO
        SharedWorkflowDTO sharedWorkflow = new SharedWorkflowDTO("Shared workflow description", true, 1, null);

        when(projectWorkflowFacade.getSharedWorkflow(anyString()))
            .thenReturn(sharedWorkflow);

        // When & Then
        this.graphQlTester
            .documentName("sharedWorkflow")
            .variable("workflowUuid", "123e4567-e89b-12d3-a456-426614174000")
            .execute()
            .path("sharedWorkflow.description")
            .entity(String.class)
            .isEqualTo("Shared workflow description")
            .path("sharedWorkflow.exported")
            .entity(Boolean.class)
            .isEqualTo(true);
    }

    @Test
    void testImportWorkflowTemplate() {
        // Given
        when(projectWorkflowFacade.importWorkflowTemplate(eq(42L), eq("tpl-1"), eq(true))).thenReturn(999L);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    importWorkflowTemplate(id: \"tpl-1\", projectId: 42, sharedWorkflow: true)
                }
                """)
            .execute()
            .path("importWorkflowTemplate")
            .entity(Long.class)
            .isEqualTo(999L);
    }

    @Test
    void testPreBuiltWorkflowTemplates() {
        // Given
        WorkflowTemplateDTO w1 = new WorkflowTemplateDTO(
            null, null, null, null, List.of("cat"), List.of(), "W Desc 1", "w-tpl-1", Instant.now(), 1, null,
            new WorkflowTemplateDTO.WorkflowInfo("WF Label 1", "WF D1"));
        WorkflowTemplateDTO w2 = new WorkflowTemplateDTO(
            null, null, null, null, List.of("cat"), List.of(), "W Desc 2", "w-tpl-2", Instant.now(), 1, null,
            new WorkflowTemplateDTO.WorkflowInfo("WF Label 2", "WF D2"));

        when(projectWorkflowFacade.getPreBuiltWorkflowTemplates(anyString(), anyString())).thenReturn(List.of(w1, w2));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    preBuiltWorkflowTemplates(query: \"\", category: \"\") {
                        id
                        description
                        workflow { label }
                    }
                }
                """)
            .execute()
            .path("preBuiltWorkflowTemplates")
            .entityList(Object.class)
            .hasSize(2)
            .path("preBuiltWorkflowTemplates[0].workflow.label")
            .entity(String.class)
            .isEqualTo("WF Label 1");
    }

    @Test
    void testWorkflowTemplate() {
        // Given
        WorkflowTemplateDTO dto = new WorkflowTemplateDTO(
            null, null, null, null, List.of(), List.of(), "WT Desc", "w-tpl-3", Instant.now(), 2, null,
            new WorkflowTemplateDTO.WorkflowInfo("Main", "Desc"));

        when(projectWorkflowFacade.getWorkflowTemplate(eq("w-tpl-3"), eq(true))).thenReturn(dto);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    workflowTemplate(id: \"w-tpl-3\", sharedWorkflow: true) {
                        id
                        description
                        projectVersion
                        workflow { label }
                    }
                }
                """)
            .execute()
            .path("workflowTemplate.id")
            .entity(String.class)
            .isEqualTo("w-tpl-3")
            .path("workflowTemplate.projectVersion")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("workflowTemplate.workflow.label")
            .entity(String.class)
            .isEqualTo("Main");
    }
}
