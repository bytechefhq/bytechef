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

import com.bytechef.platform.notification.domain.NotificationEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matija Petanjek
 */
public class NotificationHandlerContext {

    public record JobSummary(
        Long id, String name, String status, Instant startDate, Instant endDate, Long durationMillis) {
    }

    public record WorkflowSummary(String id, String name) {
    }

    public record ProjectSummary(Long id, String name, Integer version) {
    }

    public record ErrorSummary(String taskName, String taskType, String message, String stackTrace) {
    }

    public record ApprovalSummary(String expiresAt, String formUrl) {
    }

    public static class Builder {

        public NotificationHandlerContext build() {
            NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext();

            notificationHandlerContext.approvalExpiresAt = approvalExpiresAt;
            notificationHandlerContext.approvalFormUrl = approvalFormUrl;
            notificationHandlerContext.eventType = eventType;
            notificationHandlerContext.jobId = jobId;
            notificationHandlerContext.jobName = jobName;
            notificationHandlerContext.deliveryId = deliveryId;
            notificationHandlerContext.timestamp = timestamp;
            notificationHandlerContext.source = source;
            notificationHandlerContext.environment = environment;
            notificationHandlerContext.executionUrl = executionUrl;
            notificationHandlerContext.job = job;
            notificationHandlerContext.workflow = workflow;
            notificationHandlerContext.project = project;
            notificationHandlerContext.error = error;

            return notificationHandlerContext;
        }

        public Builder approvalExpiresAt(String approvalExpiresAt) {
            this.approvalExpiresAt = approvalExpiresAt;

            return this;
        }

        public Builder approvalFormUrl(String approvalFormUrl) {
            this.approvalFormUrl = approvalFormUrl;

            return this;
        }

        public Builder eventType(NotificationEvent.Type eventType) {
            this.eventType = eventType;

            return this;
        }

        public Builder jobId(Long jobId) {
            this.jobId = jobId;

            return this;
        }

        public Builder jobName(String jobName) {
            this.jobName = jobName;

            return this;
        }

        public Builder deliveryId(String deliveryId) {
            this.deliveryId = deliveryId;

            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;

            return this;
        }

        public Builder source(String source) {
            this.source = source;

            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;

            return this;
        }

        public Builder executionUrl(String executionUrl) {
            this.executionUrl = executionUrl;

            return this;
        }

        public Builder job(JobSummary job) {
            this.job = job;

            return this;
        }

        public Builder workflow(WorkflowSummary workflow) {
            this.workflow = workflow;

            return this;
        }

        public Builder project(ProjectSummary project) {
            this.project = project;

            return this;
        }

        public Builder error(ErrorSummary error) {
            this.error = error;

            return this;
        }

        private String approvalExpiresAt;
        private String approvalFormUrl;
        private NotificationEvent.Type eventType;
        private Long jobId;
        private String jobName;
        private String deliveryId;
        private Instant timestamp;
        private String source;
        private String environment;
        private String executionUrl;
        private JobSummary job;
        private WorkflowSummary workflow;
        private ProjectSummary project;
        private ErrorSummary error;
    }

    public static NotificationHandlerContext sample() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");

        return new Builder().eventType(NotificationEvent.Type.JOB_FAILED)
            .jobId(1L)
            .jobName("Example workflow")
            .timestamp(timestamp)
            .deliveryId("00000000-0000-0000-0000-000000000000")
            .source("AUTOMATION")
            .environment("PRODUCTION")
            .executionUrl("https://example.com/automation/executions?id=1")
            .job(new JobSummary(1L, "Example workflow", "FAILED", timestamp, timestamp, 0L))
            .workflow(new WorkflowSummary("example-workflow", "Example workflow"))
            .project(new ProjectSummary(1L, "Example project", 1))
            .error(
                new ErrorSummary(
                    "Send email", "email/v1/send", "Example error: connection refused",
                    "java.net.ConnectException: Example error: connection refused"))
            .build();
    }

    public String getApprovalExpiresAt() {
        return approvalExpiresAt;
    }

    public String getApprovalFormUrl() {
        return approvalFormUrl;
    }

    public NotificationEvent.Type getEventType() {
        return eventType;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getExecutionUrl() {
        return executionUrl;
    }

    public JobSummary getJob() {
        return job;
    }

    public WorkflowSummary getWorkflow() {
        return workflow;
    }

    public ProjectSummary getProject() {
        return project;
    }

    public ErrorSummary getError() {
        return error;
    }

    public ApprovalSummary getApproval() {
        if (approvalExpiresAt == null && approvalFormUrl == null) {
            return null;
        }

        return new ApprovalSummary(approvalExpiresAt, approvalFormUrl);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("eventType", eventType == null ? null : eventType.name());
        map.put("jobId", jobId);
        map.put("jobName", jobName);

        boolean approvalEvent = eventType == NotificationEvent.Type.JOB_APPROVAL_EXPIRING
            || eventType == NotificationEvent.Type.JOB_APPROVAL_ESCALATED;

        if (approvalEvent || approvalExpiresAt != null) {
            map.put("approvalExpiresAt", approvalExpiresAt);
        }

        if (approvalEvent || approvalFormUrl != null) {
            map.put("approvalFormUrl", approvalFormUrl);
        }

        Instant resolvedTimestamp = timestamp == null ? Instant.now() : timestamp;

        map.put("timestamp", resolvedTimestamp.toString());

        putIfPresent(map, "deliveryId", deliveryId);
        putIfPresent(map, "source", source);

        if (job != null) {
            Map<String, Object> jobMap = new LinkedHashMap<>();

            putIfPresent(jobMap, "id", job.id());
            putIfPresent(jobMap, "name", job.name());
            putIfPresent(jobMap, "status", job.status());
            putIfPresent(jobMap, "startDate", job.startDate() == null ? null : job.startDate()
                .toString());
            putIfPresent(jobMap, "endDate", job.endDate() == null ? null : job.endDate()
                .toString());
            putIfPresent(jobMap, "durationMillis", job.durationMillis());

            map.put("job", jobMap);
        }

        if (workflow != null) {
            Map<String, Object> workflowMap = new LinkedHashMap<>();

            putIfPresent(workflowMap, "id", workflow.id());
            putIfPresent(workflowMap, "name", workflow.name());

            map.put("workflow", workflowMap);
        }

        if (project != null) {
            Map<String, Object> projectMap = new LinkedHashMap<>();

            putIfPresent(projectMap, "id", project.id());
            putIfPresent(projectMap, "name", project.name());
            putIfPresent(projectMap, "version", project.version());

            map.put("project", projectMap);
        }

        putIfPresent(map, "environment", environment);

        if (error != null) {
            Map<String, Object> errorMap = new LinkedHashMap<>();

            putIfPresent(errorMap, "taskName", error.taskName());
            putIfPresent(errorMap, "taskType", error.taskType());
            putIfPresent(errorMap, "message", error.message());
            putIfPresent(errorMap, "stackTrace", error.stackTrace());

            map.put("error", errorMap);
        }

        putIfPresent(map, "executionUrl", executionUrl);

        ApprovalSummary approval = getApproval();

        if (approval != null) {
            Map<String, Object> approvalMap = new LinkedHashMap<>();

            putIfPresent(approvalMap, "expiresAt", approval.expiresAt());
            putIfPresent(approvalMap, "formUrl", approval.formUrl());

            map.put("approval", approvalMap);
        }

        return map;
    }

    public String toDetailsText() {
        List<String> lines = new ArrayList<>();

        if (workflow != null && workflow.name() != null) {
            lines.add("Workflow: " + workflow.name());
        }

        if (project != null && project.name() != null) {
            lines.add(
                "Project: " + project.name() + (project.version() == null ? "" : " (v" + project.version() + ")"));
        }

        if (environment != null) {
            lines.add("Environment: " + environment);
        }

        if (error != null && error.taskName() != null) {
            lines.add(
                "Failed task: " + error.taskName() + (error.taskType() == null ? "" : " (" + error.taskType() + ")"));
        }

        if (error != null && error.message() != null) {
            lines.add("Error: " + error.message());
        }

        if (executionUrl != null) {
            lines.add("Execution: " + executionUrl);
        }

        return String.join("\n", lines);
    }

    public NotificationHandlerContext withDeliveryId(String deliveryId) {
        NotificationHandlerContext copy = copy();

        copy.deliveryId = deliveryId;

        return copy;
    }

    public NotificationHandlerContext withTimestamp(Instant timestamp) {
        NotificationHandlerContext copy = copy();

        copy.timestamp = timestamp;

        return copy;
    }

    private NotificationHandlerContext() {

    }

    private String approvalExpiresAt;
    private String approvalFormUrl;
    private NotificationEvent.Type eventType;
    private Long jobId;
    private String jobName;
    private String deliveryId;
    private Instant timestamp;
    private String source;
    private String environment;
    private String executionUrl;
    private JobSummary job;
    private WorkflowSummary workflow;
    private ProjectSummary project;
    private ErrorSummary error;

    private NotificationHandlerContext copy() {
        NotificationHandlerContext copy = new NotificationHandlerContext();

        copy.approvalExpiresAt = approvalExpiresAt;
        copy.approvalFormUrl = approvalFormUrl;
        copy.eventType = eventType;
        copy.jobId = jobId;
        copy.jobName = jobName;
        copy.deliveryId = deliveryId;
        copy.timestamp = timestamp;
        copy.source = source;
        copy.environment = environment;
        copy.executionUrl = executionUrl;
        copy.job = job;
        copy.workflow = workflow;
        copy.project = project;
        copy.error = error;

        return copy;
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
