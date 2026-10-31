/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
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

    private static final byte[] PROJECT_BYTES = "({name: 'p', workflows: []})".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnvironmentService environmentService;

    @MockitoBean
    private ProjectCodeWorkflowFacade projectCodeWorkflowFacade;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();

        when(environmentService.getEnvironment(nullable(String.class))).thenCallRealMethod();
    }

    @Test
    void testDeployProject() {
        MultipartBodyBuilder multipartBodyBuilder = multipartBody();

        multipartBodyBuilder.part("workspaceId", "7");

        deploy(multipartBodyBuilder)
            .expectStatus()
            .isNoContent();

        verify(projectCodeWorkflowFacade).save(
            eq(7L), eq(PROJECT_BYTES), eq(Language.JAVASCRIPT), eq(Environment.PRODUCTION), eq(false));
    }

    @Test
    void testDeployProjectPassesTheEnableFlagThrough() {
        MultipartBodyBuilder multipartBodyBuilder = multipartBody();

        multipartBodyBuilder.part("workspaceId", "7");

        deploy(multipartBodyBuilder, "STAGING", "/v1/projects/deploy?enable=true")
            .expectStatus()
            .isNoContent();

        verify(projectCodeWorkflowFacade).save(
            eq(7L), eq(PROJECT_BYTES), eq(Language.JAVASCRIPT), eq(Environment.STAGING), eq(true));
    }

    @Test
    void testDeployProjectWithoutWorkspaceIdIsBadRequest() {
        deploy(multipartBody())
            .expectStatus()
            .isBadRequest();

        verify(projectCodeWorkflowFacade, never()).save(anyLong(), any(), any(), any(), anyBoolean());
    }

    @Test
    void testDeployProjectAcceptsTheEnvironmentHeaderCaseInsensitively() {
        MultipartBodyBuilder multipartBodyBuilder = multipartBody();

        multipartBodyBuilder.part("workspaceId", "7");

        deploy(multipartBodyBuilder, "staging")
            .expectStatus()
            .isNoContent();

        verify(projectCodeWorkflowFacade).save(
            eq(7L), eq(PROJECT_BYTES), eq(Language.JAVASCRIPT), eq(Environment.STAGING), eq(false));
    }

    @Test
    void testDeployProjectRejectsAnUnknownEnvironmentHeader() {
        MultipartBodyBuilder multipartBodyBuilder = multipartBody();

        multipartBodyBuilder.part("workspaceId", "7");

        deploy(multipartBodyBuilder, "nowhere")
            .expectStatus()
            .isBadRequest();

        verify(projectCodeWorkflowFacade, never()).save(anyLong(), any(), any(), any(), anyBoolean());
    }

    private WebTestClient.ResponseSpec deploy(MultipartBodyBuilder multipartBodyBuilder) {
        return deploy(multipartBodyBuilder, null);
    }

    private WebTestClient.ResponseSpec deploy(MultipartBodyBuilder multipartBodyBuilder, String environment) {
        return deploy(multipartBodyBuilder, environment, "/v1/projects/deploy");
    }

    private WebTestClient.ResponseSpec deploy(
        MultipartBodyBuilder multipartBodyBuilder, String environment, String uri) {

        WebTestClient.RequestBodySpec requestBodySpec = webTestClient
            .post()
            .uri(uri)
            .contentType(MediaType.MULTIPART_FORM_DATA);

        if (environment != null) {
            requestBodySpec.header("X-Environment", environment);
        }

        return requestBodySpec
            .bodyValue(multipartBodyBuilder.build())
            .exchange();
    }

    private static MultipartBodyBuilder multipartBody() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

        multipartBodyBuilder.part("projectFile", new ByteArrayResource(PROJECT_BYTES) {

            @Override
            public String getFilename() {
                return "project.js";
            }
        });

        return multipartBodyBuilder;
    }
}
