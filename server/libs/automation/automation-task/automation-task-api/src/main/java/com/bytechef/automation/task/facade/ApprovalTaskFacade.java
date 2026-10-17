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

package com.bytechef.automation.task.facade;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.PendingApproval;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Facade for cross-service approval task operations that span the task module and other domain services.
 *
 * @author Ivica Cardic
 */
public interface ApprovalTaskFacade {

    /**
     * Creates an approval task, deriving its {@code environment} from the deployment that owns the task's resume job.
     *
     * @param approvalTask the approval task to create; {@code jobResumeId} must be set
     * @return the persisted approval task
     * @throws IllegalArgumentException if the resume token is invalid or expired
     */
    ApprovalTask createApprovalTask(ApprovalTask approvalTask);

    List<PendingApproval> getPendingApprovals(@Nullable Integer environmentId);
}
