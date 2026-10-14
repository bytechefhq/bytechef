/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = ProjectCodeWorkflowApiController.class)
@WebMvcTest(controllers = ProjectCodeWorkflowApiController.class, properties = "bytechef.edition=ee")
class ProjectCodeWorkflowApiControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectCodeWorkflowFacade projectCodeWorkflowFacade;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    void testDeployProjectJavaScript() {
        byte[] bytes = "export function handler() {}".getBytes(StandardCharsets.UTF_8);

        deployProject("project.js", bytes);

        verify(projectCodeWorkflowFacade).save(eq(7L), eq(bytes), eq(Language.JAVASCRIPT));
    }

    @Test
    void testDeployProjectPython() {
        byte[] bytes = "def handler(): pass".getBytes(StandardCharsets.UTF_8);

        deployProject("project.py", bytes);

        verify(projectCodeWorkflowFacade).save(eq(7L), eq(bytes), eq(Language.PYTHON));
    }

    @Test
    void testDeployProjectJava() {
        byte[] bytes = {
            0x50, 0x4b, 0x03, 0x04
        };

        deployProject("project.jar", bytes);

        verify(projectCodeWorkflowFacade).save(eq(7L), eq(bytes), eq(Language.JAVA));
    }

    @Test
    void testDeployProjectWithoutWorkspaceId() {
        MultipartBodyBuilder multipartBodyBuilder = multipartBody("project.js", new byte[] {
            1
        });

        webTestClient
            .post()
            .uri("/internal/projects/deploy")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(multipartBodyBuilder.build())
            .exchange()
            .expectStatus()
            .isBadRequest();
    }

    private void deployProject(String filename, byte[] bytes) {
        MultipartBodyBuilder multipartBodyBuilder = multipartBody(filename, bytes);

        multipartBodyBuilder.part("workspaceId", "7");

        webTestClient
            .post()
            .uri("/internal/projects/deploy")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(multipartBodyBuilder.build())
            .exchange()
            .expectStatus()
            .isNoContent();
    }

    private static MultipartBodyBuilder multipartBody(String filename, byte[] bytes) {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

        multipartBodyBuilder.part("projectFile", new ByteArrayResource(bytes) {

            @Override
            public String getFilename() {
                return filename;
            }
        });

        return multipartBodyBuilder;
    }
}
