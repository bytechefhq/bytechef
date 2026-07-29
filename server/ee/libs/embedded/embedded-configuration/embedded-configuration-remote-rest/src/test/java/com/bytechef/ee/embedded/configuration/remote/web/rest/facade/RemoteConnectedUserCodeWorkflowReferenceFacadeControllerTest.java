/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.facade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = RemoteConnectedUserCodeWorkflowReferenceFacadeControllerTest.TestConfig.class)
@WebMvcTest(controllers = RemoteConnectedUserCodeWorkflowReferenceFacadeController.class)
class RemoteConnectedUserCodeWorkflowReferenceFacadeControllerTest {

    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class TestConfig {

        @Bean
        RemoteConnectedUserCodeWorkflowReferenceFacadeController
            remoteConnectedUserCodeWorkflowReferenceFacadeController(
                ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade) {

            return new RemoteConnectedUserCodeWorkflowReferenceFacadeController(
                connectedUserCodeWorkflowReferenceFacade);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Test
    void testGetConnectedUserWorkflowsReturnsFacadeResult() throws Exception {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setId(1L);
        connectedUserProjectWorkflow.setConnectedUserProjectId(2L);

        when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(42L))
            .thenReturn(List.of(connectedUserProjectWorkflow));

        mockMvc.perform(get("/remote/connected-user-code-workflow-reference-facade/get-connected-user-workflows/42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetOrCreateReferenceReturns409OnMissingConnection() throws Exception {
        when(connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            eq("ext-1"), eq("uuid-1"), any(Environment.class)))
                .thenThrow(new MissingConnectionException("slack"));

        mockMvc.perform(
            post("/remote/connected-user-code-workflow-reference-facade/get-or-create-reference")
                .param("externalUserId", "ext-1")
                .param("catalogWorkflowUuid", "uuid-1")
                .param("environment", "PRODUCTION"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.missingConnectionComponentName").value("slack"));
    }
}
