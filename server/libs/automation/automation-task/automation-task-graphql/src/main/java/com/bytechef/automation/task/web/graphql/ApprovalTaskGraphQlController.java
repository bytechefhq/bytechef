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

package com.bytechef.automation.task.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.ApprovalTask.Priority;
import com.bytechef.automation.task.domain.ApprovalTask.Status;
import com.bytechef.automation.task.service.ApprovalTaskService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ApprovalTaskGraphQlController {

    private final ApprovalTaskService approvalTaskService;

    @SuppressFBWarnings("EI")
    public ApprovalTaskGraphQlController(ApprovalTaskService approvalTaskService) {
        this.approvalTaskService = approvalTaskService;
    }

    @MutationMapping
    @PreAuthorize("isTenantAdmin()")
    public ApprovalTask createApprovalTask(@Argument ApprovalTaskInput approvalTask) {
        return approvalTaskService.create(toApprovalTask(approvalTask));
    }

    @MutationMapping
    @PreAuthorize("hasPermission(#id, 'ApprovalTask', 'DEPLOYMENT_EDIT')")
    public boolean deleteApprovalTask(@Argument long id) {
        approvalTaskService.delete(id);

        return true;
    }

    @QueryMapping
    @PreAuthorize("hasPermission(#id, 'ApprovalTask', 'DEPLOYMENT_VIEW') or isResourceOwner(#id, 'ApprovalTask')")
    public ApprovalTask approvalTask(@Argument long id) {
        return approvalTaskService.getApprovalTask(id);
    }

    @QueryMapping
    @PostFilter("isTenantAdmin() or (filterObject.assigneeId != null and isCurrentUser(filterObject.assigneeId))")
    public List<ApprovalTask> approvalTasks(@Argument Integer environmentId) {
        return approvalTaskService.getApprovalTasks(environmentId);
    }

    @QueryMapping
    @PostFilter("hasPermission(filterObject.id, 'ApprovalTask', 'DEPLOYMENT_VIEW') or " +
        "isResourceOwner(filterObject.id, 'ApprovalTask')")
    public List<ApprovalTask> approvalTasksByIds(@Argument List<Long> ids) {
        return approvalTaskService.getApprovalTasks(ids);
    }

    @MutationMapping
    @PreAuthorize("hasPermission(#approvalTask.id, 'ApprovalTask', 'DEPLOYMENT_EDIT') or " +
        "isResourceOwner(#approvalTask.id, 'ApprovalTask')")
    public ApprovalTask updateApprovalTask(@Argument ApprovalTaskInput approvalTask) {
        return approvalTaskService.update(toApprovalTask(approvalTask));
    }

    private ApprovalTask toApprovalTask(ApprovalTaskInput approvalTaskInput) {
        return ApprovalTask.builder()
            .id(approvalTaskInput.id())
            .name(approvalTaskInput.name())
            .description(approvalTaskInput.description())
            .status(approvalTaskInput.status())
            .priority(approvalTaskInput.priority())
            .assigneeId(approvalTaskInput.assigneeId())
            .dueDate(approvalTaskInput.dueDate() == null ? null : Instant.parse(approvalTaskInput.dueDate()))
            .version(approvalTaskInput.version() == null ? 0 : approvalTaskInput.version())
            .build();
    }

    public record ApprovalTaskInput(
        Long id, String name, String description, Status status, Priority priority, Long assigneeId, String dueDate,
        Integer version) {
    }
}
