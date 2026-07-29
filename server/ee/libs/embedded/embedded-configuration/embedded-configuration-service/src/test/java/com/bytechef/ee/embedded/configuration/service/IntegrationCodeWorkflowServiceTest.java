/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.ee.embedded.configuration.repository.IntegrationCodeWorkflowRepository;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class IntegrationCodeWorkflowServiceTest {

    @Mock
    private IntegrationCodeWorkflowRepository integrationCodeWorkflowRepository;

    @Test
    void testCreate() {
        IntegrationCodeWorkflowService integrationCodeWorkflowService =
            new IntegrationCodeWorkflowServiceImpl(integrationCodeWorkflowRepository);

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(UUID.randomUUID());

        codeWorkflowContainer.setId(3L);

        Integration integration = Integration.builder()
            .id(1L)
            .name("integration")
            .componentName("integrationComponent")
            .version(1)
            .build();

        integrationCodeWorkflowService.create(codeWorkflowContainer, integration);

        ArgumentCaptor<IntegrationCodeWorkflow> captor = ArgumentCaptor.forClass(IntegrationCodeWorkflow.class);

        verify(integrationCodeWorkflowRepository).save(captor.capture());

        IntegrationCodeWorkflow saved = captor.getValue();

        assertThat(saved.getIntegrationId()).isEqualTo(1L);
        assertThat(saved.getIntegrationVersion()).isEqualTo(integration.getLastIntegrationVersion());
        assertThat(saved.getCodeWorkflowContainerId()).isEqualTo(3L);
    }

    @Test
    void testFetchIntegrationCodeWorkflow() {
        IntegrationCodeWorkflowService integrationCodeWorkflowService =
            new IntegrationCodeWorkflowServiceImpl(integrationCodeWorkflowRepository);

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(UUID.randomUUID());

        codeWorkflowContainer.setId(3L);

        Integration integration = Integration.builder()
            .id(1L)
            .name("integration")
            .componentName("integrationComponent")
            .version(1)
            .build();

        IntegrationCodeWorkflow integrationCodeWorkflow = new IntegrationCodeWorkflow();

        integrationCodeWorkflow.setId(10L);
        integrationCodeWorkflow.setCodeWorkflowContainer(codeWorkflowContainer);
        integrationCodeWorkflow.setIntegration(integration);
        integrationCodeWorkflow.setIntegrationVersion(1);

        when(integrationCodeWorkflowRepository.findFirstByIntegrationIdOrderByIdDesc(1L))
            .thenReturn(Optional.of(integrationCodeWorkflow));

        Optional<IntegrationCodeWorkflow> fetchedIntegrationCodeWorkflow =
            integrationCodeWorkflowService.fetchIntegrationCodeWorkflow(1L);

        assertThat(fetchedIntegrationCodeWorkflow).contains(integrationCodeWorkflow);
    }
}
