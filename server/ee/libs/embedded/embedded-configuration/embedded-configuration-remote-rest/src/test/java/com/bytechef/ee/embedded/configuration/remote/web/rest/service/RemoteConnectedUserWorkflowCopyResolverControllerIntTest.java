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

import com.bytechef.automation.configuration.service.ConnectedUserWorkflowCopyResolver;
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
    classes = RemoteConnectedUserWorkflowCopyResolverControllerIntTest.RemoteControllerTestConfiguration.class)
@WebMvcTest(RemoteConnectedUserWorkflowCopyResolverController.class)
class RemoteConnectedUserWorkflowCopyResolverControllerIntTest {

    @MockitoBean
    private ConnectedUserWorkflowCopyResolver connectedUserWorkflowCopyResolver;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetWorkflowCopyUuids() throws Exception {
        when(connectedUserWorkflowCopyResolver.getWorkflowCopyUuids("template-uuid"))
            .thenReturn(List.of("copy-uuid-1", "copy-uuid-2"));

        mockMvc
            .perform(
                get("/remote/connected-user-workflow-copy-resolver/get-workflow-copy-uuids/template-uuid")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json("[\"copy-uuid-1\",\"copy-uuid-2\"]", JsonCompareMode.STRICT));
    }

    @Configuration
    @Import(RemoteConnectedUserWorkflowCopyResolverController.class)
    static class RemoteControllerTestConfiguration {
    }
}
