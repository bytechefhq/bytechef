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

package com.bytechef.automation.workflow.execution.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 */
class JobVisibilityProviderTest {

    private final JobService jobService = mock(JobService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final JobVisibilityProvider provider = new JobVisibilityProvider(jobService, projectService);

    private ListAppender<ILoggingEvent> logAppender;

    @SuppressWarnings("PMD")
    private ch.qos.logback.classic.Logger providerLogger;

    @BeforeEach
    void setUp() {
        providerLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(JobVisibilityProvider.class);
        logAppender = new ListAppender<>();

        logAppender.start();
        providerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        providerLogger.detachAppender(logAppender);
    }

    @Test
    void testJobInheritsProjectRecord() {
        Job job = mock(Job.class);
        Project project = new Project();

        project.setId(3L);
        project.setVisibility(ResourceVisibility.PRIVATE);

        ReflectionTestUtils.setField(project, "createdBy", "ivica");

        when(job.getWorkflowId()).thenReturn("wf");
        when(jobService.fetchJob(11L)).thenReturn(Optional.of(job));
        when(projectService.fetchWorkflowProject("wf")).thenReturn(Optional.of(project));

        assertThat(provider.resourceType()).isEqualTo("Job");
        assertThat(provider.visibilityResourceType()).isEqualTo("Project");
        assertThat(provider.fetchVisibility(11L))
            .contains(new VisibilityRecord(3L, ResourceVisibility.PRIVATE, "ivica"));
    }

    /**
     * A platform or embedded job is an ordinary absence, so it must go through the non-throwing lookup and must not
     * produce an operator-facing log line — otherwise the error channel fills with noise and the genuine failure below
     * is indistinguishable from it.
     */
    @Test
    void testNonProjectJobFailsClosedWithoutLogging() {
        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn("platform-wf");
        when(jobService.fetchJob(12L)).thenReturn(Optional.of(job));
        when(projectService.fetchWorkflowProject("platform-wf")).thenReturn(Optional.empty());

        assertThat(provider.fetchVisibility(12L)).isEmpty();
        assertThat(logAppender.list).isEmpty();

        verify(projectService, never()).getWorkflowProject(anyString());
    }

    @Test
    void testUnknownJobFailsClosed() {
        when(jobService.fetchJob(13L)).thenReturn(Optional.empty());

        assertThat(provider.fetchVisibility(13L)).isEmpty();
    }

    /**
     * The lookup failing is an infrastructure failure. It must still deny, and it must leave an operator something to
     * look at naming the workflow and carrying the cause.
     */
    @Test
    void testLookupFailureFailsClosedAndLogsError() {
        Job job = mock(Job.class);
        RuntimeException cause = new IllegalStateException("connection pool exhausted");

        when(job.getWorkflowId()).thenReturn("wf-14");
        when(jobService.fetchJob(14L)).thenReturn(Optional.of(job));
        when(projectService.fetchWorkflowProject("wf-14")).thenThrow(cause);

        assertThat(provider.fetchVisibility(14L)).isEmpty();

        List<ILoggingEvent> errorEvents = logAppender.list.stream()
            .filter(loggingEvent -> loggingEvent.getLevel() == Level.ERROR)
            .toList();

        assertThat(errorEvents).hasSize(1);
        assertThat(errorEvents.getFirst()
            .getFormattedMessage()).contains("wf-14");
        assertThat(errorEvents.getFirst()
            .getThrowableProxy()
            .getMessage()).isEqualTo("connection pool exhausted");
    }
}
