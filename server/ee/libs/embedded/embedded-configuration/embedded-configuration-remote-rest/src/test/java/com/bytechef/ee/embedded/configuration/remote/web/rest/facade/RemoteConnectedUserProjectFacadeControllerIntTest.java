/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.facade;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(
    classes = RemoteConnectedUserProjectFacadeControllerIntTest.RemoteControllerTestConfiguration.class)
@WebMvcTest(RemoteConnectedUserProjectFacadeController.class)
class RemoteConnectedUserProjectFacadeControllerIntTest {

    @MockitoBean
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCopyWorkflowTemplate() throws Exception {
        when(connectedUserProjectFacade.copyWorkflowTemplate("user-1", "template-uuid", Environment.PRODUCTION))
            .thenReturn("copy-uuid");

        mockMvc
            .perform(
                post("/remote/connected-user-project-facade/copy-workflow-template")
                    .queryParam("externalUserId", "user-1")
                    .queryParam("workflowUuid", "template-uuid")
                    .queryParam("environment", "PRODUCTION"))
            .andExpect(status().isOk())
            .andExpect(content().string("copy-uuid"));
    }

    @Test
    void testCopyWorkflowTemplateRequiresEnvironment() throws Exception {
        mockMvc
            .perform(
                post("/remote/connected-user-project-facade/copy-workflow-template")
                    .queryParam("externalUserId", "user-1")
                    .queryParam("workflowUuid", "template-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Configuration
    @Import(RemoteConnectedUserProjectFacadeController.class)
    static class RemoteControllerTestConfiguration {
    }
}
