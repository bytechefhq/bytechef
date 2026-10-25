/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.web.rest.facade;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.execution.facade.JobFacade;
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
@ContextConfiguration(classes = RemoteJobFacadeControllerIntTest.RemoteControllerTestConfiguration.class)
@WebMvcTest(RemoteJobFacadeController.class)
class RemoteJobFacadeControllerIntTest {

    @MockitoBean
    private JobFacade jobFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testResumeClaimedJob() throws Exception {
        mockMvc
            .perform(
                post("/remote/job-facade/resume-claimed-job")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":7,\"taskExecutionId\":11,\"data\":{\"status\":\"COMPLETED\"}}"))
            .andExpect(status().isNoContent());

        verify(jobFacade).resumeClaimedJob(7L, 11L, Map.of("status", "COMPLETED"));
    }

    @Test
    void testResumeClaimedJobWithoutTaskExecutionIdAndData() throws Exception {
        mockMvc
            .perform(
                post("/remote/job-facade/resume-claimed-job")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":7}"))
            .andExpect(status().isNoContent());

        verify(jobFacade).resumeClaimedJob(eq(7L), isNull(), isNull());
    }

    @Configuration
    @Import(RemoteJobFacadeController.class)
    static class RemoteControllerTestConfiguration {
    }
}
