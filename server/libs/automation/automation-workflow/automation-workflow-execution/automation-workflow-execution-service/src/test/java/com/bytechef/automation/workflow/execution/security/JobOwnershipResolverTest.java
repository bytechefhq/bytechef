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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class JobOwnershipResolverTest {

    private final JobService jobService = mock(JobService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final JobOwnershipResolver resolver = new JobOwnershipResolver(jobService, projectService);

    private ListAppender<ILoggingEvent> logAppender;

    @SuppressWarnings("PMD")
    private ch.qos.logback.classic.Logger resolverLogger;

    @BeforeEach
    void setUp() {
        resolverLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(JobOwnershipResolver.class);
        logAppender = new ListAppender<>();

        logAppender.start();
        resolverLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        resolverLogger.detachAppender(logAppender);
    }

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("Job");
    }

    @Test
    void testResolvesWorkspaceViaWorkflowProject() {
        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn("wf-1");
        when(jobService.fetchJob(1L)).thenReturn(Optional.of(job));

        Project project = mock(Project.class);

        when(project.getWorkspaceId()).thenReturn(42L);
        when(projectService.fetchWorkflowProject("wf-1")).thenReturn(Optional.of(project));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(42L);
    }

    @Test
    void testUnknownJobIsUnknown() {
        when(jobService.fetchJob(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
    }

    /**
     * A platform or embedded job is an ordinary absence, so it must go through the non-throwing lookup and must not
     * produce an operator-facing log line — otherwise the error channel fills with noise and the genuine failure below
     * is indistinguishable from it.
     */
    @Test
    void testNonProjectWorkflowFailsClosedWithoutLogging() {
        Job job = mock(Job.class);

        when(job.getWorkflowId()).thenReturn("platform-wf");
        when(jobService.fetchJob(2L)).thenReturn(Optional.of(job));
        when(projectService.fetchWorkflowProject("platform-wf")).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(2L)
            .workspaceId()).isEmpty();

        assertThat(logAppender.list).isEmpty();

        verify(projectService, never()).getWorkflowProject(anyString());
    }

    /**
     * The lookup failing is an infrastructure failure. It must still deny, and it must leave an operator something to
     * look at naming the workflow and carrying the cause.
     */
    @Test
    void testLookupFailureFailsClosedAndLogsError() {
        Job job = mock(Job.class);
        RuntimeException cause = new IllegalStateException("connection pool exhausted");

        when(job.getWorkflowId()).thenReturn("wf-3");
        when(jobService.fetchJob(3L)).thenReturn(Optional.of(job));
        when(projectService.fetchWorkflowProject("wf-3")).thenThrow(cause);

        assertThat(resolver.resolveOwner(3L)
            .workspaceId()).isEmpty();

        List<ILoggingEvent> errorEvents = logAppender.list.stream()
            .filter(loggingEvent -> loggingEvent.getLevel() == Level.ERROR)
            .toList();

        assertThat(errorEvents).hasSize(1);
        assertThat(errorEvents.getFirst()
            .getFormattedMessage()).contains("wf-3");
        assertThat(errorEvents.getFirst()
            .getThrowableProxy()
            .getMessage()).isEqualTo("connection pool exhausted");
    }
}
