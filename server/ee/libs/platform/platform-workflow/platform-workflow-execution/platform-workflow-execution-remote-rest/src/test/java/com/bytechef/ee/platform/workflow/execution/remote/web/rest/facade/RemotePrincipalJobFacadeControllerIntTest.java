/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.web.rest.facade;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = RemotePrincipalJobFacadeControllerIntTest.RemoteControllerTestConfiguration.class)
@WebMvcTest(RemotePrincipalJobFacadeController.class)
class RemotePrincipalJobFacadeControllerIntTest {

    @MockitoBean
    private PrincipalJobFacade principalJobFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCreatePrincipalLinkedJobForPrincipal() throws Exception {
        when(
            principalJobFacade.createPrincipalLinkedJob(
                eq(new JobParametersDTO("subflow-uuid", Map.of("a", 1))), eq(5L), eq(PlatformType.AUTOMATION)))
                    .thenReturn(42L);

        mockMvc
            .perform(
                post("/remote/principal-job-facade/create-principal-linked-job-for-principal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"jobParameters\":{\"workflowId\":\"subflow-uuid\",\"inputs\":{\"a\":1}}," +
                            "\"jobPrincipalId\":5,\"type\":\"AUTOMATION\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string("42"));
    }

    @Configuration
    @Import(RemotePrincipalJobFacadeController.class)
    static class RemoteControllerTestConfiguration {
    }
}
