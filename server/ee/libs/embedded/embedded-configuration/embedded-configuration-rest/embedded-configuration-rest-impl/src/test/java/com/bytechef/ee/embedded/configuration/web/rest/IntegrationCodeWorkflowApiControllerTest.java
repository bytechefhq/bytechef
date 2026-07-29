/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.embedded.configuration.facade.IntegrationCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationCodeWorkflowApiControllerTest {

    private final IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade =
        mock(IntegrationCodeWorkflowFacade.class);

    private final IntegrationCodeWorkflowApiController integrationCodeWorkflowApiController =
        new IntegrationCodeWorkflowApiController(integrationCodeWorkflowFacade);

    @Test
    void testDeployIntegrationJavaScript() {
        byte[] bytes = "export function handler() {}".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile integrationFile = new MockMultipartFile(
            "integrationFile", "integration.js", "application/javascript", bytes);

        ResponseEntity<Void> responseEntity = integrationCodeWorkflowApiController.deployIntegration(
            integrationFile);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(integrationCodeWorkflowFacade).save(eq(bytes), eq(Language.JAVASCRIPT));
    }

    @Test
    void testDeployIntegrationPython() {
        byte[] bytes = "def handler(): pass".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile integrationFile = new MockMultipartFile(
            "integrationFile", "integration.py", "text/x-python", bytes);

        integrationCodeWorkflowApiController.deployIntegration(integrationFile);

        verify(integrationCodeWorkflowFacade).save(eq(bytes), eq(Language.PYTHON));
    }

    @Test
    void testDeployIntegrationRuby() {
        byte[] bytes = "def handler; end".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile integrationFile = new MockMultipartFile(
            "integrationFile", "integration.rb", "application/x-ruby", bytes);

        integrationCodeWorkflowApiController.deployIntegration(integrationFile);

        verify(integrationCodeWorkflowFacade).save(eq(bytes), eq(Language.RUBY));
    }

    @Test
    void testDeployIntegrationJava() {
        byte[] bytes = {
            0x50, 0x4b, 0x03, 0x04
        };

        MockMultipartFile integrationFile = new MockMultipartFile(
            "integrationFile", "integration.jar", "application/java-archive", bytes);

        integrationCodeWorkflowApiController.deployIntegration(integrationFile);

        verify(integrationCodeWorkflowFacade).save(eq(bytes), eq(Language.JAVA));
    }
}
