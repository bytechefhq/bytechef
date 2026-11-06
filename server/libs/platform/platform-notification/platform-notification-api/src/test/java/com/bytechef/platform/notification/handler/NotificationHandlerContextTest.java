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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.ErrorSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.JobSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.ProjectSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.WorkflowSummary;
import java.time.Instant;
import java.util.LinkedHashMap;
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
    void testWithTimestampCopiesEveryField() {
        NotificationHandlerContext sample = NotificationHandlerContext.sample();
        Instant timestamp = Instant.parse("2027-02-03T04:05:06Z");

        NotificationHandlerContext copy = sample.withTimestamp(timestamp);

        assertEquals(timestamp, copy.getTimestamp());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), sample.getTimestamp());

        Map<String, Object> expected = new LinkedHashMap<>(sample.toMap());

        expected.put("timestamp", timestamp.toString());

        assertEquals(expected, copy.toMap());
        assertEquals(sample.getDeliveryId(), copy.getDeliveryId());
        assertEquals(sample.getJobId(), copy.getJobId());
        assertEquals(sample.getEnvironment(), copy.getEnvironment());
        assertEquals(sample.getExecutionUrl(), copy.getExecutionUrl());
    }

    @Test
    void testSampleFillsEverySection() {
        NotificationHandlerContext sample = NotificationHandlerContext.sample();

        Map<String, Object> map = sample.toMap();

        for (String key : List.of(
            "job", "workflow", "project", "environment", "error", "executionUrl", "source", "deliveryId")) {

            assertTrue(map.containsKey(key), key);
        }

        assertEquals(NotificationEvent.Type.JOB_FAILED, sample.getEventType());
    }

    @Test
    void testWithDeliveryIdCopiesEveryOtherField() {
        NotificationHandlerContext context = new NotificationHandlerContext.Builder()
            .eventType(NotificationEvent.Type.JOB_FAILED)
            .jobId(42L)
            .jobName("Daily sync")
            .timestamp(Instant.parse("2026-11-03T10:00:00Z"))
            .deliveryId("d-1")
            .source("AUTOMATION")
            .environment("PRODUCTION")
            .executionUrl("https://x/e")
            .build();

        NotificationHandlerContext copy = context.withDeliveryId("d-2");

        Map<String, Object> expected = new LinkedHashMap<>(context.toMap());

        expected.put("deliveryId", "d-2");

        assertEquals("d-2", copy.getDeliveryId());
        assertEquals("d-1", context.getDeliveryId());
        assertEquals(expected, copy.toMap());
        assertEquals(context.getTimestamp(), copy.getTimestamp());
        assertEquals(context.getJobId(), copy.getJobId());
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
            .executionUrl("https://x/automation/workflow-executions?id=42")
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
            .executionUrl("https://x/automation/workflow-executions?id=42")
            .build();

        assertEquals(
            "Workflow: Daily sync\nProject: CRM (v3)\nEnvironment: PRODUCTION\n" +
                "Failed task: Send email (email/v1/send)\nError: boom\nExecution: https://x/automation/workflow-executions?id=42",
            full.toDetailsText());

        NotificationHandlerContext partial = new NotificationHandlerContext.Builder()
            .environment("PRODUCTION")
            .build();

        assertEquals("Environment: PRODUCTION", partial.toDetailsText());
        assertEquals("", new NotificationHandlerContext.Builder().build()
            .toDetailsText());
    }
}
