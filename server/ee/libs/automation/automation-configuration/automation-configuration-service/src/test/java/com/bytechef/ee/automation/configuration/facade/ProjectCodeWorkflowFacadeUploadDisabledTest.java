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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
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
            codeWorkflowContainerFacade, projectCodeWorkflowService, mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), mock(TagService.class), mock(WorkflowService.class), List.of());

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
            codeWorkflowContainerFacade, projectCodeWorkflowService, mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), mock(TagService.class), mock(WorkflowService.class), List.of());

        // A JavaScript upload gets past the Java-only disable guard; it may still fail downstream on the empty payload,
        // but never with the disabled ConfigurationException.
        assertThatThrownBy(() -> projectCodeWorkflowFacade.save(1L, new byte[0], Language.JAVASCRIPT))
            .isNotInstanceOf(ConfigurationException.class);
    }

    /**
     * {@code javaEnabled} is deliberately TRUE here. With it false, a rejection would be explained equally well by the
     * Java guard above, and this test would pass whether or not the Ruby guard exists. Enabling Java removes that
     * alternative, so the only thing that can reject a Ruby upload is the guard under test.
     *
     * <p>
     * RUBY-DISABLED: delete this test when Ruby is restored. Until then it closes the last hole in the disable —
     * create-empty refused Ruby and the client dropped {@code .rb} from its accept lists, but a direct API caller could
     * still deploy a Ruby workflow the polyglot loader cannot run.
     */
    @Test
    void testSaveRejectsRubyUploadEvenWhenJavaIsEnabled() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), projectService, projectWorkflowService,
            codeWorkflowContainerFacade, projectCodeWorkflowService, mock(CodeWorkflowContainerService.class),
            mock(CodeWorkflowFileStorage.class), mock(TagService.class), mock(WorkflowService.class), List.of());

        assertThatThrownBy(() -> projectCodeWorkflowFacade.save(1L, new byte[0], Language.RUBY))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(thrown -> {
                ConfigurationException configurationException = (ConfigurationException) thrown;

                // 101 is LANGUAGE_NOT_SUPPORTED, not the 100 the Java guard raises -- asserting the key is what
                // distinguishes the two rejections.
                assertThat(configurationException.getErrorKey()).isEqualTo(101);
                assertThat(configurationException.getEntityClass()).isEqualTo(CodeWorkflowContainer.class);
            });

        verifyNoInteractions(
            projectService, projectWorkflowService, codeWorkflowContainerFacade, projectCodeWorkflowService);
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
