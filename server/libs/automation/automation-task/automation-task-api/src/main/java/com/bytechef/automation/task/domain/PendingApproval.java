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

package com.bytechef.automation.task.domain;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * A workflow run currently paused on a human approval, regardless of which channels delivered the request. Feeds the
 * pending-approvals inbox: unlike {@link ApprovalTask} rows (created only by the approval-task channel), this view is
 * derived live from STOPPED jobs carrying a resume id, so every blocked run is visible.
 *
 * @param jobId         the paused run's job id
 * @param workflowLabel the workflow's display label (falls back to the workflow id)
 * @param formUrl       the hosted approval form URL, when the public URL is configured
 * @param createdDate   when the run started
 * @param expiresAt     when the pending approval expires, when the suspend carries an expiry
 * @author Ivica Cardic
 */
public record PendingApproval(
    long jobId, String workflowLabel, @Nullable String formUrl, @Nullable Instant createdDate,
    @Nullable Instant expiresAt) {
}
