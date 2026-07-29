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

/**
 * @author Matija Petanjek
 */
public class NotificationHandlerContext {

    public static class Builder {

        public NotificationHandlerContext build() {
            NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext();

            notificationHandlerContext.approvalExpiresAt = approvalExpiresAt;
            notificationHandlerContext.approvalFormUrl = approvalFormUrl;
            notificationHandlerContext.eventType = eventType;
            notificationHandlerContext.jobId = jobId;
            notificationHandlerContext.jobName = jobName;

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

        private String approvalExpiresAt;
        private String approvalFormUrl;
        private NotificationEvent.Type eventType;
        private Long jobId;
        private String jobName;
    }

    /**
     * ISO-8601 instant at which the pending approval expires; set only for approval-related event types.
     */
    public String getApprovalExpiresAt() {
        return approvalExpiresAt;
    }

    /**
     * Hosted approval form URL for the pending approval; set only for approval-related event types when a public URL is
     * configured.
     */
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

    private NotificationHandlerContext() {

    }

    private String approvalExpiresAt;
    private String approvalFormUrl;
    private NotificationEvent.Type eventType;
    private Long jobId;
    private String jobName;

}
