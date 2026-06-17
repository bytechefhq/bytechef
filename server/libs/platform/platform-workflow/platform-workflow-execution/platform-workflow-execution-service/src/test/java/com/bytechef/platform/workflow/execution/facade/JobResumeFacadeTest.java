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

package com.bytechef.platform.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.tenant.TenantContext;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class JobResumeFacadeTest {

    private static final long JOB_ID = 42L;
    private static final long TASK_EXECUTION_ID = 7L;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private JobFacade jobFacade;

    @Mock
    private JobService jobService;

    private JobResumeFacadeImpl jobResumeFacade;

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        MapUtils.setObjectMapper(objectMapper);
    }

    @BeforeEach
    void setUp() {
        jobResumeFacade = new JobResumeFacadeImpl(applicationEventPublisher, jobFacade, jobService);
    }

    @Test
    public void testResumeJobReturnsInvalidIdForUnparseableToken() {
        JobResumeOutcome outcome = jobResumeFacade.resumeJob("not-a-token", Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsGoneWhenJobNotStopped() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.COMPLETED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.GONE);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsInvalidIdWhenStoredMetadataMissing() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, null);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsInvalidIdWhenUuidMismatch() {
        JobResumeId suppliedJobResumeId = JobResumeId.of(JOB_ID);
        JobResumeId storedJobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, storedJobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(suppliedJobResumeId.toString(), Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsInvalidIdWhenTenantMismatch() {
        String currentTenantId = TenantContext.getCurrentTenantId();
        UUID sharedUuid = UUID.randomUUID();

        String suppliedToken = encodeToken(currentTenantId, JOB_ID, sharedUuid);
        String storedToken = encodeToken(currentTenantId + "-other", JOB_ID, sharedUuid);

        Job job = jobOf(Job.Status.STOPPED, storedToken);

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(suppliedToken, Map.of());

        assertThat(outcome).isEqualTo(JobResumeOutcome.INVALID_ID);

        verify(jobFacade, never()).resumeJob(anyLong(), anyLong(), anyMap());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testResumeJobReturnsOkWhenTokenMatches() {
        JobResumeId jobResumeId = JobResumeId.of(JOB_ID);

        Job job = jobOf(Job.Status.STOPPED, jobResumeId.toString());

        when(jobService.getJob(JOB_ID)).thenReturn(job);

        Map<String, Object> data = Map.of("foo", "bar");

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(jobResumeId.toString(), data);

        assertThat(outcome).isEqualTo(JobResumeOutcome.OK);

        verify(jobFacade).resumeJob(JOB_ID, TASK_EXECUTION_ID, data);
        verify(applicationEventPublisher).publishEvent(any(JobResumedEvent.class));
    }

    private static Job jobOf(Job.Status status, String storedJobResumeIdString) {
        Job job = new Job(JOB_ID);

        job.setStatus(status);

        if (storedJobResumeIdString != null) {
            Map<String, Object> metadata = new HashMap<>();

            metadata.put(MetadataConstants.JOB_RESUME_ID, storedJobResumeIdString);
            metadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, TASK_EXECUTION_ID);

            job.setMetadata(metadata);
        }

        return job;
    }

    private static String encodeToken(String tenantId, long jobId, UUID uuid) {
        return EncodingUtils.base64EncodeToString(tenantId + ":" + jobId + ":" + uuid);
    }
}
