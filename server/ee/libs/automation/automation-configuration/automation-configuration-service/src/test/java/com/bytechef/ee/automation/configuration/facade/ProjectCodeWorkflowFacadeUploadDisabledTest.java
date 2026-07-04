/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.exception.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

/**
 * Verifies that {@link ProjectCodeWorkflowFacadeImpl#save} rejects Java (jar) uploads when they are disabled via
 * {@code bytechef.workflow.code-workflow.java-enabled=false}, short-circuiting before any persistence, while uploads in
 * other languages are not gated by the flag.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectCodeWorkflowFacadeUploadDisabledTest {

    @Test
    void testSaveRejectsJavaUploadWhenJavaDisabled() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(false), mock(CacheManager.class), projectService, projectWorkflowService,
            codeWorkflowContainerFacade, projectCodeWorkflowService);

        assertThatThrownBy(() -> projectCodeWorkflowFacade.save(1L, new byte[0], Language.JAVA))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(thrown -> {
                ConfigurationException configurationException = (ConfigurationException) thrown;

                assertThat(configurationException.getErrorKey()).isEqualTo(100);
                assertThat(configurationException.getEntityClass()).isEqualTo(CodeWorkflowContainer.class);
            });

        verifyNoInteractions(
            projectService, projectWorkflowService, codeWorkflowContainerFacade, projectCodeWorkflowService);
    }

    @Test
    void testSaveDoesNotGateNonJavaUploadWhenJavaDisabled() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(false), mock(CacheManager.class), projectService, projectWorkflowService,
            codeWorkflowContainerFacade, projectCodeWorkflowService);

        // A JavaScript upload gets past the Java-only disable guard; it may still fail downstream on the empty payload,
        // but never with the disabled ConfigurationException.
        assertThatThrownBy(() -> projectCodeWorkflowFacade.save(1L, new byte[0], Language.JAVASCRIPT))
            .isNotInstanceOf(ConfigurationException.class);
    }

    private static ApplicationProperties applicationProperties(boolean javaEnabled) {
        ApplicationProperties.Workflow workflow = new ApplicationProperties.Workflow();

        workflow.getCodeWorkflow()
            .setJavaEnabled(javaEnabled);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getWorkflow()).thenReturn(workflow);

        return applicationProperties;
    }
}
