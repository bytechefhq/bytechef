/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.workflow.execution.web.rest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = JobApiController.class)
@WebMvcTest(controllers = JobApiController.class, properties = "bytechef.coordinator.enabled=true")
class JobApiControllerIntTest {

    private static final long JOB_ID = 42L;

    @MockitoBean
    private JobFacade jobFacade;

    @MockitoBean
    private JobResumeFacade jobResumeFacade;

    @MockitoBean
    private JobService jobService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRestartJobRestartsAStoppedJob() throws Exception {
        when(jobService.getJob(JOB_ID)).thenReturn(stoppedJob(Map.of()));

        mockMvc.perform(put("/internal/jobs/{id}/restart", JOB_ID))
            .andExpect(status().isNoContent());

        verify(jobFacade).resumeJob(JOB_ID);
    }

    @Test
    void testRestartJobRejectsAJobWaitingOnAnApproval() throws Exception {
        when(jobService.getJob(JOB_ID)).thenReturn(
            stoppedJob(Map.of(MetadataConstants.JOB_RESUME_ID, "resumeId", MetadataConstants.APPROVAL_RESUME, true)));

        mockMvc.perform(put("/internal/jobs/{id}/restart", JOB_ID))
            .andExpect(status().isConflict());

        verify(jobFacade, never()).resumeJob(anyLong());
    }

    @Test
    void testStopJobCancelsAJobWaitingOnAnApproval() throws Exception {
        when(jobResumeFacade.cancelApproval(JOB_ID)).thenReturn(true);

        mockMvc.perform(put("/internal/jobs/{id}/stop", JOB_ID))
            .andExpect(status().isNoContent());

        verify(jobFacade, never()).stopJob(anyLong());
    }

    @Test
    void testStopJobStopsARunningJob() throws Exception {
        when(jobResumeFacade.cancelApproval(JOB_ID)).thenReturn(false);

        mockMvc.perform(put("/internal/jobs/{id}/stop", JOB_ID))
            .andExpect(status().isNoContent());

        verify(jobFacade).stopJob(JOB_ID);
    }

    private static Job stoppedJob(Map<String, Object> metadata) {
        Job job = new Job(JOB_ID);

        job.setMetadata(metadata);
        job.setStatus(Job.Status.STOPPED);

        return job;
    }
}
