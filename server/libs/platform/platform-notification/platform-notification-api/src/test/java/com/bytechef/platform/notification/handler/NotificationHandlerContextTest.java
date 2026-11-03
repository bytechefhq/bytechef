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

package com.bytechef.platform.notification.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.ApprovalSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.ErrorSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.JobSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.ProjectSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.WorkflowSummary;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class NotificationHandlerContextTest {

    @Test
    void testToMapKeepsLegacyKeysFirstAndOmitsAbsentSections() {
        NotificationHandlerContext context = new NotificationHandlerContext.Builder()
            .eventType(NotificationEvent.Type.JOB_FAILED)
            .jobId(42L)
            .jobName("Daily sync")
            .timestamp(Instant.parse("2026-11-03T10:00:00Z"))
            .build();

        Map<String, Object> map = context.toMap();

        assertEquals(List.of("eventType", "jobId", "jobName", "timestamp"), List.copyOf(map.keySet()));
        assertEquals("JOB_FAILED", map.get("eventType"));
        assertEquals("2026-11-03T10:00:00Z", map.get("timestamp"));
    }

    @Test
    void testToMapDefaultsTimestampWhenNotSet() {
        NotificationHandlerContext context = new NotificationHandlerContext.Builder()
            .eventType(NotificationEvent.Type.JOB_FAILED)
            .build();

        assertFalse(((String) context.toMap()
            .get("timestamp")).isEmpty());
        assertNull(context.getTimestamp());
    }

    @Test
    void testToMapIncludesApprovalKeysAtTopLevelAndNested() {
        NotificationHandlerContext context = new NotificationHandlerContext.Builder()
            .eventType(NotificationEvent.Type.JOB_FAILED)
            .jobId(42L)
            .jobName("Daily sync")
            .approvalExpiresAt("2026-11-04T10:00:00Z")
            .approvalFormUrl("https://x/resume/t")
            .timestamp(Instant.parse("2026-11-03T10:00:00Z"))
            .build();

        Map<String, Object> map = context.toMap();

        assertEquals(
            List.of(
                "eventType", "jobId", "jobName", "approvalExpiresAt", "approvalFormUrl", "timestamp", "approval"),
            List.copyOf(map.keySet()));
        assertEquals("https://x/resume/t", ((Map<?, ?>) map.get("approval")).get("formUrl"));
        assertEquals(new ApprovalSummary("2026-11-04T10:00:00Z", "https://x/resume/t"), context.getApproval());
    }

    @Test
    void testToMapNestsSummariesAndOmitsNullFields() {
        Instant start = Instant.parse("2026-11-03T09:00:00Z");

        NotificationHandlerContext context = new NotificationHandlerContext.Builder()
            .eventType(NotificationEvent.Type.JOB_FAILED)
            .jobId(42L)
            .jobName("Daily sync")
            .timestamp(Instant.parse("2026-11-03T10:00:00Z"))
            .deliveryId("d-1")
            .source("AUTOMATION")
            .job(new JobSummary(42L, "Daily sync", "FAILED", start, null, null))
            .workflow(new WorkflowSummary("wf-1", "Daily sync"))
            .project(new ProjectSummary(1L, "CRM", 3))
            .environment("PRODUCTION")
            .error(new ErrorSummary("Send email", "email/v1/send", null, "java.io.IOException"))
            .executionUrl("https://x/automation/executions?id=42")
            .build();

        Map<String, Object> map = context.toMap();

        assertEquals(
            List.of(
                "eventType", "jobId", "jobName", "timestamp", "deliveryId", "source", "job", "workflow", "project",
                "environment", "error", "executionUrl"),
            List.copyOf(map.keySet()));
        assertEquals("d-1", map.get("deliveryId"));
        assertEquals("AUTOMATION", map.get("source"));

        Map<?, ?> job = (Map<?, ?>) map.get("job");

        assertEquals(List.of("id", "name", "status", "startDate"), List.copyOf(job.keySet()));
        assertEquals("2026-11-03T09:00:00Z", job.get("startDate"));

        Map<?, ?> error = (Map<?, ?>) map.get("error");

        assertFalse(error.containsKey("message"));
        assertEquals(List.of("taskName", "taskType", "stackTrace"), List.copyOf(error.keySet()));
        assertEquals(Map.of("id", "wf-1", "name", "Daily sync"), map.get("workflow"));
        assertEquals(Map.of("id", 1L, "name", "CRM", "version", 3), map.get("project"));
    }

    @Test
    void testToDetailsTextSkipsAbsentLinesAndIsEmptyWhenNothingIsSet() {
        NotificationHandlerContext full = new NotificationHandlerContext.Builder()
            .workflow(new WorkflowSummary("wf-1", "Daily sync"))
            .project(new ProjectSummary(1L, "CRM", 3))
            .environment("PRODUCTION")
            .error(new ErrorSummary("Send email", "email/v1/send", "boom", null))
            .executionUrl("https://x/automation/executions?id=42")
            .build();

        assertEquals(
            "Workflow: Daily sync\nProject: CRM (v3)\nEnvironment: PRODUCTION\n" +
                "Failed task: Send email (email/v1/send)\nError: boom\nExecution: https://x/automation/executions?id=42",
            full.toDetailsText());

        NotificationHandlerContext partial = new NotificationHandlerContext.Builder()
            .environment("PRODUCTION")
            .build();

        assertEquals("Environment: PRODUCTION", partial.toDetailsText());
        assertEquals("", new NotificationHandlerContext.Builder().build()
            .toDetailsText());
    }
}
