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

package com.bytechef.automation.task.service;

import com.bytechef.automation.task.domain.ApprovalTask;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ApprovalTaskService {

    long countApprovalTasks();

    ApprovalTask create(ApprovalTask approvalTask);

    void delete(long id);

    Optional<ApprovalTask> fetchApprovalTask(String name);

    Optional<ApprovalTask> fetchApprovalTaskByJobResumeId(String jobResumeId);

    ApprovalTask getApprovalTask(long id);

    List<ApprovalTask> getApprovalTasks();

    List<ApprovalTask> getApprovalTasks(Integer environmentId);

    List<ApprovalTask> getApprovalTasks(List<Long> ids);

    List<ApprovalTask> getUnresolvedApprovalTasks();

    /**
     * Persists the resolution the caller applied with {@link ApprovalTask#complete()} or {@link ApprovalTask#expire()}.
     *
     * @param approvalTask the resolved approval task
     * @return the persisted approval task
     * @throws IllegalStateException if the stored task is already resolved
     */
    ApprovalTask resolve(ApprovalTask approvalTask);

    /**
     * Updates the editable fields of a task; its status may only move between OPEN and IN_PROGRESS.
     *
     * @param approvalTask the approval task to update
     * @return the updated approval task
     * @throws IllegalStateException if the update would change the status of a resolved task or resolve it
     */
    ApprovalTask update(ApprovalTask approvalTask);
}
