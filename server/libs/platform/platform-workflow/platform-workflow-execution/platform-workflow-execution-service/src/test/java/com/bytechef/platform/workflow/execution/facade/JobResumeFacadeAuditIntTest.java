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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.audit.JobApprovalAuditEvents;
import com.bytechef.platform.workflow.execution.audit.JobApprovalAuditMapper;
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@Import({
    JobFacadeImpl.class, JobResumeFacadeImpl.class, JobServiceImpl.class, TaskExecutionServiceImpl.class
})
@SpringBootTest(
    classes = {
        PlatformWorkflowExecutionIntTestConfiguration.class, JobResumeFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true"
    })
class JobResumeFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final long MISSING_JOB_ID = 999_999L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @DynamicPropertySource
    static void signingSecretProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        byte[] secretBytes = new byte[32];

        SECURE_RANDOM.nextBytes(secretBytes);

        dynamicPropertyRegistry.add("bytechef.approval.signed-token.clock-skew", () -> "PT60S");
        dynamicPropertyRegistry.add("bytechef.approval.signed-token.default-ttl", () -> "P30D");
        dynamicPropertyRegistry.add(
            "bytechef.approval.signed-token.secret", () -> EncodingUtils.base64EncodeToString(secretBytes));
    }

    @MockitoBean
    private ContextService contextService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobResumeFacade jobResumeFacade;

    @Autowired
    private JobService jobService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockitoBean
    private TaskFileStorage taskFileStorage;

    @Autowired
    private TaskStateFacade taskStateFacade;

    @MockitoBean
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_LOGIN, "n/a", List.of()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        jdbcTemplate.update("DELETE FROM task_state");
        jdbcTemplate.update("DELETE FROM job");

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCancelApprovalRecordsTheJob() {
        long jobId = saveJob(true);

        taskStateFacade.save(JobResumeId.parse(getStoredJobResumeId(jobId)), new Suspend(Map.of(), null));

        assertThat(jobResumeFacade.cancelApproval(jobId)).isTrue();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(JobApprovalAuditEvents.JOB_APPROVAL_CANCELLED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("jobId", String.valueOf(jobId));
    }

    @Test
    void testCancelApprovalOfAJobThatDoesNotWaitOnAnApprovalIsNotRecorded() {
        long jobId = saveJob(false);

        assertThat(jobResumeFacade.cancelApproval(jobId)).isFalse();

        assertThat(findEvents(JobApprovalAuditEvents.JOB_APPROVAL_CANCELLED)).isEmpty();
    }

    @Test
    void testCancelApprovalOfAnUnknownJobRecordsAnError() {
        assertThatThrownBy(() -> jobResumeFacade.cancelApproval(MISSING_JOB_ID))
            .isInstanceOf(RuntimeException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(JobApprovalAuditEvents.JOB_APPROVAL_CANCELLED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("jobId", String.valueOf(MISSING_JOB_ID));
    }

    private List<PersistentAuditEvent> findEvents(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> pageContent = page.getContent();

        return pageContent.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        List<PersistentAuditEvent> persistentAuditEvents = findEvents(eventType);

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private String getStoredJobResumeId(long jobId) {
        Job job = jobService.getJob(jobId);

        return (String) job.getMetadata(MetadataConstants.JOB_RESUME_ID);
    }

    private long saveJob(boolean approval) {
        Job job = new Job();

        job.setStatus(Job.Status.STOPPED);
        job.setWorkflowId("workflow-1");

        job = jobRepository.save(job);

        long jobId = Validate.notNull(job.getId(), "id");

        Map<String, Object> metadata = new HashMap<>();

        metadata.put(MetadataConstants.JOB_RESUME_ID, JobResumeId.of(jobId)
            .toString());

        if (approval) {
            metadata.put(MetadataConstants.APPROVAL_RESUME, true);
        }

        job = jobService.getJob(jobId);

        job.setMetadata(metadata);

        jobService.update(job);

        return jobId;
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class,
        JobApprovalAuditMapper.class
    })
    static class AuditConfiguration {
    }
}
