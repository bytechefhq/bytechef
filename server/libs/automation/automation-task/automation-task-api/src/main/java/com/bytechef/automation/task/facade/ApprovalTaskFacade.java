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

import com.bytechef.automation.task.dto.ApprovalTaskDTO;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ApprovalTaskFacade {

    long countApprovalTasks();

    ApprovalTaskDTO createApprovalTask(ApprovalTaskDTO approvalTaskDTO);

    void deleteApprovalTask(long id);

    Optional<ApprovalTaskDTO> fetchApprovalTask(String name);

    ApprovalTaskDTO getApprovalTask(long id);

    List<ApprovalTaskDTO> getApprovalTasks();

    List<ApprovalTaskDTO> getApprovalTasks(List<Long> ids);

    ApprovalTaskDTO updateApprovalTask(ApprovalTaskDTO approvalTaskDTO);
}
