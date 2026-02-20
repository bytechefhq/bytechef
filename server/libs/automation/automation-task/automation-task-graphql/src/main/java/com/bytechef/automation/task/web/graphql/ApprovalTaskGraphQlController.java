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
import com.bytechef.automation.task.dto.ApprovalTaskDTO;
import com.bytechef.automation.task.facade.ApprovalTaskFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ApprovalTaskGraphQlController {

    private final ApprovalTaskFacade approvalTaskFacade;

    @SuppressFBWarnings("EI")
    public ApprovalTaskGraphQlController(ApprovalTaskFacade approvalTaskFacade) {
        this.approvalTaskFacade = approvalTaskFacade;
    }

    @MutationMapping
    public ApprovalTaskDTO createApprovalTask(@Argument ApprovalTaskInput approvalTask) {
        return approvalTaskFacade.createApprovalTask(toApprovalTaskDTO(approvalTask));
    }

    @MutationMapping
    public boolean deleteApprovalTask(@Argument long id) {
        approvalTaskFacade.deleteApprovalTask(id);

        return true;
    }

    @QueryMapping
    public ApprovalTaskDTO approvalTask(@Argument long id) {
        return approvalTaskFacade.getApprovalTask(id);
    }

    @QueryMapping
    public List<ApprovalTaskDTO> approvalTasks() {
        return approvalTaskFacade.getApprovalTasks();
    }

    @QueryMapping
    public List<ApprovalTaskDTO> approvalTasksByIds(@Argument List<Long> ids) {
        return approvalTaskFacade.getApprovalTasks(ids);
    }

    @MutationMapping
    public ApprovalTaskDTO updateApprovalTask(@Argument ApprovalTaskInput approvalTask) {
        return approvalTaskFacade.updateApprovalTask(toApprovalTaskDTO(approvalTask));
    }

    private ApprovalTaskDTO toApprovalTaskDTO(ApprovalTaskInput approvalTaskInput) {
        return new ApprovalTaskDTO(
            null,
            null,
            approvalTaskInput.description(),
            approvalTaskInput.id(),
            null,
            null,
            approvalTaskInput.name(),
            approvalTaskInput.version() == null ? 0 : approvalTaskInput.version());
    }

    record ApprovalTaskInput(Long id, String name, String description, Integer version) {
    }
}
