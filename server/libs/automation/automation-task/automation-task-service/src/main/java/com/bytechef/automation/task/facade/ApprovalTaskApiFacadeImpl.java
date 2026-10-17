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
import com.bytechef.automation.task.service.ApprovalTaskService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ApprovalTaskApiFacadeImpl implements ApprovalTaskApiFacade {

    private final ApprovalTaskFacade approvalTaskFacade;
    private final ApprovalTaskService approvalTaskService;

    @SuppressFBWarnings("EI")
    public ApprovalTaskApiFacadeImpl(ApprovalTaskFacade approvalTaskFacade, ApprovalTaskService approvalTaskService) {
        this.approvalTaskFacade = approvalTaskFacade;
        this.approvalTaskService = approvalTaskService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public ApprovalTask createApprovalTask(ApprovalTask approvalTask) {
        return approvalTaskService.create(approvalTask);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteApprovalTask(long id) {
        approvalTaskService.delete(id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public ApprovalTask getApprovalTask(long id) {
        return approvalTaskService.getApprovalTask(id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<ApprovalTask> getApprovalTasks(@Nullable Integer environmentId) {
        return approvalTaskService.getApprovalTasks(environmentId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<ApprovalTask> getApprovalTasks(List<Long> ids) {
        return approvalTaskService.getApprovalTasks(ids);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<PendingApproval> getPendingApprovals(@Nullable Integer environmentId) {
        return approvalTaskFacade.getPendingApprovals(environmentId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public ApprovalTask updateApprovalTask(ApprovalTask approvalTask) {
        return approvalTaskService.update(approvalTask);
    }
}
