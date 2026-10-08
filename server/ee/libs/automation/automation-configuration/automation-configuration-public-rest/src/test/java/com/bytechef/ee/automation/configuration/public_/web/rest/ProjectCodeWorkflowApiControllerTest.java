/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectCodeWorkflowApiControllerTest {

    private final ProjectCodeWorkflowFacade projectCodeWorkflowFacade = mock(ProjectCodeWorkflowFacade.class);
    private final ProjectCodeWorkflowApiController projectCodeWorkflowApiController =
        new ProjectCodeWorkflowApiController(projectCodeWorkflowFacade);

    @Test
    void testDeployProjectSavesIntoTheWorkspaceTheRequestNames() {
        MockMultipartFile projectFile = new MockMultipartFile(
            "projectFile", "orders.jar", "application/java-archive", new byte[] {
                1, 2, 3
            });

        projectCodeWorkflowApiController.deployProject(42L, projectFile);

        verify(projectCodeWorkflowFacade).save(eq(42L), eq(new byte[] {
            1, 2, 3
        }), eq(Language.JAVA));
    }

    @Test
    void testDeployProjectRejectsAMissingWorkspace() {
        MockMultipartFile projectFile = new MockMultipartFile(
            "projectFile", "orders.jar", "application/java-archive", new byte[0]);

        assertThatThrownBy(() -> projectCodeWorkflowApiController.deployProject(null, projectFile))
            .isInstanceOf(NullPointerException.class);

        verify(projectCodeWorkflowFacade, never()).save(anyLong(), any(), any());
    }
}
