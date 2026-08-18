/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that {@link ConnectedUserProjectFacadeImpl#getConnectedUserProjectWorkflows(String, Environment)} appends
 * automation-bridge reference rows (see {@link ConnectedUserCodeWorkflowReferenceFacade}) to the caller's own copy-mode
 * workflow list, tagging each with {@link ConnectedUserProjectWorkflowDTO.Kind#REFERENCE} and the catalog-derived
 * label/description/components -- and that a reference whose catalog workflow is no longer served (dangling) still
 * appears, falling back to the catalog uuid as its label.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ConnectedUserProjectFacadeWorkflowListTest {

    private static final String EXTERNAL_USER_ID = "ext-1";

    @Mock
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Mock
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Mock
    private ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;

    @Mock
    private ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private JobService jobService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowComponentResolver workflowComponentResolver;

    @Mock
    private WorkflowService workflowService;

    private ConnectedUserProjectFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new ConnectedUserProjectFacadeImpl(
            automationWorkflowProjectFacade, null, null, connectedUserCodeWorkflowReferenceFacade,
            connectedUserProjectWorkflowManager, null, connectedUserProjectWorkflowService, connectedUserService,
            null, null, null, null, null, jobService, null, null, null, null, null, projectService, null,
            projectWorkflowService, workflowComponentResolver, null, workflowService, null, null);

        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);
        connectedUserProject.setConnectedUserId(7L);
        connectedUserProject.setProjectId(20L);

        when(connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(EXTERNAL_USER_ID, Environment.PRODUCTION))
                .thenReturn(connectedUserProject);

        Project project = new Project();

        project.setId(20L);

        when(projectService.getProject(20L)).thenReturn(project);

        // Overridden by testGetConnectedUserProjectWorkflowsCarriesCopiedFromWorkflowUuidOnCopyRowsOnly, which needs
        // a non-empty copy-mode workflow list -- lenient so that override doesn't fail strict-stubbing checks here.
        lenient().when(projectWorkflowService.getProjectWorkflows(eq(20L), eq(1)))
            .thenReturn(List.of());

        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "user@example.com", true, EXTERNAL_USER_ID, 7L,
            "Test User", 0);

        when(connectedUserService.getConnectedUser(7L)).thenReturn(connectedUser);
    }

    @Test
    void testGetConnectedUserProjectWorkflowsAppendsReferenceRows() {
        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(99L);
        reference.setCatalogWorkflowUuid("cat-1");
        reference.setEnabled(true);
        reference.setDangling(false);

        when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(7L)).thenReturn(List.of(reference));

        ConnectedUserWorkflowTemplateDTO template = new ConnectedUserWorkflowTemplateDTO(
            "cat-1", "Sync leads", "d", null, List.of(),
            List.of(new ConnectedUserWorkflowTemplateDTO.Component("slack", "Slack", "icon")), null);

        AutomationWorkflowProjectDTO catalogProject = new AutomationWorkflowProjectDTO(
            1L, "Catalog", "desc", null, List.of(), true, 1, 1, List.of(template), null, true);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(catalogProject));

        List<ConnectedUserProjectWorkflowDTO> result =
            facade.getConnectedUserProjectWorkflows(EXTERNAL_USER_ID, Environment.PRODUCTION);

        assertThat(result).hasSize(1);

        ConnectedUserProjectWorkflowDTO connectedUserProjectWorkflowDTO = result.getFirst();

        assertThat(connectedUserProjectWorkflowDTO.kind()).isEqualTo(ConnectedUserProjectWorkflowDTO.Kind.REFERENCE);
        assertThat(connectedUserProjectWorkflowDTO.catalogWorkflowUuid()).isEqualTo("cat-1");
        assertThat(connectedUserProjectWorkflowDTO.workflowUuid()).isEqualTo("cat-1");
        assertThat(connectedUserProjectWorkflowDTO.dangling()).isFalse();
        assertThat(connectedUserProjectWorkflowDTO.workflow()
            .getLabel()).isEqualTo("Sync leads");
        assertThat(connectedUserProjectWorkflowDTO.components()).extracting("name")
            .containsExactly("slack");
    }

    @Test
    void testGetConnectedUserProjectWorkflowsFallsBackToUuidLabelWhenReferenceIsDangling() {
        ConnectedUserProjectWorkflow danglingReference = new ConnectedUserProjectWorkflow();

        danglingReference.setId(100L);
        danglingReference.setCatalogWorkflowUuid("cat-gone");
        danglingReference.setEnabled(false);
        danglingReference.setDangling(true);

        when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(7L))
            .thenReturn(List.of(danglingReference));

        // The catalog no longer serves "cat-gone" -- no workflow template matches it.
        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of());

        List<ConnectedUserProjectWorkflowDTO> result =
            facade.getConnectedUserProjectWorkflows(EXTERNAL_USER_ID, Environment.PRODUCTION);

        assertThat(result).hasSize(1);

        ConnectedUserProjectWorkflowDTO connectedUserProjectWorkflowDTO = result.getFirst();

        assertThat(connectedUserProjectWorkflowDTO.kind()).isEqualTo(ConnectedUserProjectWorkflowDTO.Kind.REFERENCE);
        assertThat(connectedUserProjectWorkflowDTO.catalogWorkflowUuid()).isEqualTo("cat-gone");
        assertThat(connectedUserProjectWorkflowDTO.dangling()).isTrue();
        assertThat(connectedUserProjectWorkflowDTO.workflow()
            .getLabel()).isEqualTo("cat-gone");
        assertThat(connectedUserProjectWorkflowDTO.components()).isEmpty();
    }

    @Test
    void testGetConnectedUserProjectWorkflowsCarriesCopiedFromWorkflowUuidOnCopyRowsOnly() {
        ProjectWorkflow copyProjectWorkflow = new ProjectWorkflow(50L);

        copyProjectWorkflow.setProjectVersion(1);
        copyProjectWorkflow.setWorkflowId("wf-1");

        when(projectWorkflowService.getProjectWorkflows(eq(20L), eq(1))).thenReturn(List.of(copyProjectWorkflow));

        Workflow workflow = new Workflow("wf-1", JsonUtils.write(Map.of("label", "My Copy")), Workflow.Format.JSON);

        when(workflowService.getWorkflows(List.of("wf-1"))).thenReturn(List.of(workflow));

        ConnectedUserProjectWorkflow copyEntity = new ConnectedUserProjectWorkflow();

        copyEntity.setId(200L);
        copyEntity.setCopiedFromWorkflowUuid("template-uuid");

        when(connectedUserProjectWorkflowService.getConnectedUserProjectWorkflow(10L, 50L)).thenReturn(copyEntity);

        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(99L);
        reference.setCatalogWorkflowUuid("cat-1");
        reference.setEnabled(true);
        reference.setDangling(false);

        when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(7L)).thenReturn(List.of(reference));

        List<ConnectedUserProjectWorkflowDTO> result =
            facade.getConnectedUserProjectWorkflows(EXTERNAL_USER_ID, Environment.PRODUCTION);

        assertThat(result).hasSize(2);

        ConnectedUserProjectWorkflowDTO copyDTO = result.stream()
            .filter(connectedUserProjectWorkflowDTO -> connectedUserProjectWorkflowDTO
                .kind() == ConnectedUserProjectWorkflowDTO.Kind.COPY)
            .findFirst()
            .orElseThrow();

        assertThat(copyDTO.copiedFromWorkflowUuid()).isEqualTo("template-uuid");

        ConnectedUserProjectWorkflowDTO referenceDTO = result.stream()
            .filter(connectedUserProjectWorkflowDTO -> connectedUserProjectWorkflowDTO
                .kind() == ConnectedUserProjectWorkflowDTO.Kind.REFERENCE)
            .findFirst()
            .orElseThrow();

        assertThat(referenceDTO.copiedFromWorkflowUuid()).isNull();
    }
}
