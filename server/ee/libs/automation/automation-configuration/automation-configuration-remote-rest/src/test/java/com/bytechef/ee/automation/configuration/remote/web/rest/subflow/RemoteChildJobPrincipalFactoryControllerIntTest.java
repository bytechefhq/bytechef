/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.web.rest.subflow;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
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
@ContextConfiguration(classes = RemoteChildJobPrincipalFactoryControllerIntTest.RemoteControllerTestConfiguration.class)
@WebMvcTest(RemoteChildJobPrincipalFactoryController.class)
class RemoteChildJobPrincipalFactoryControllerIntTest {

    @MockitoBean
    private ChildJobPrincipalFactory childJobPrincipalFactory;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCreatePrincipalLinkedJob() throws Exception {
        when(
            childJobPrincipalFactory.createPrincipalLinkedJob(
                eq(7L), eq(new JobParametersDTO("subflow-uuid", Map.of("a", 1)))))
                    .thenReturn(42L);

        mockMvc
            .perform(
                post("/remote/child-job-principal-factory/create-principal-linked-job")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"referenceJobId\":7,\"jobParameters\":{\"workflowId\":\"subflow-uuid\",\"inputs\":{\"a\":1}}}"))
            .andExpect(status().isOk())
            .andExpect(content().string("42"));
    }

    @Configuration
    @Import(RemoteChildJobPrincipalFactoryController.class)
    static class RemoteControllerTestConfiguration {
    }
}
