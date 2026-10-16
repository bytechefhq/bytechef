/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.service;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(
    classes = RemoteConnectedUserProjectDeploymentResolverControllerIntTest.RemoteControllerTestConfiguration.class)
@WebMvcTest(RemoteConnectedUserProjectDeploymentResolverController.class)
class RemoteConnectedUserProjectDeploymentResolverControllerIntTest {

    @MockitoBean
    private ConnectedUserProjectDeploymentResolver connectedUserProjectDeploymentResolver;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetProjectDeploymentIds() throws Exception {
        when(connectedUserProjectDeploymentResolver.getProjectDeploymentIds(7L)).thenReturn(List.of(3L, 5L, 11L));

        mockMvc
            .perform(
                get("/remote/connected-user-project-deployment-resolver/get-project-deployment-ids/7")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json("[3,5,11]", JsonCompareMode.STRICT));
    }

    @Configuration
    @Import(RemoteConnectedUserProjectDeploymentResolverController.class)
    static class RemoteControllerTestConfiguration {
    }
}
