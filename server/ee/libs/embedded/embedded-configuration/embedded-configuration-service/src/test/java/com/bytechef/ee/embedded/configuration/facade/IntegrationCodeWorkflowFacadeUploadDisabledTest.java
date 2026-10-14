/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.service.IntegrationCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationCodeWorkflowFacadeUploadDisabledTest {

    @Test
    void testSaveRejectsRubyUploadEvenWhenJavaIsEnabled() {
        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
        IntegrationCodeWorkflowService integrationCodeWorkflowService = mock(IntegrationCodeWorkflowService.class);
        IntegrationService integrationService = mock(IntegrationService.class);
        IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

        IntegrationCodeWorkflowFacadeImpl integrationCodeWorkflowFacade = new IntegrationCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), codeWorkflowContainerFacade,
            integrationCodeWorkflowService, mock(IntegrationInstanceConfigurationFacade.class),
            mock(IntegrationFacade.class),
            integrationService,
            integrationWorkflowService,
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class), mock(TagService.class),
            mock(WorkflowService.class), List.of());

        assertThatThrownBy(() -> integrationCodeWorkflowFacade.save(new byte[0], Language.RUBY))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(thrown -> {
                ConfigurationException configurationException = (ConfigurationException) thrown;

                assertThat(configurationException.getErrorKey()).isEqualTo(101);
                assertThat(configurationException.getEntityClass()).isEqualTo(CodeWorkflowContainer.class);
            });

        verifyNoInteractions(
            codeWorkflowContainerFacade, integrationCodeWorkflowService, integrationService,
            integrationWorkflowService);
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
