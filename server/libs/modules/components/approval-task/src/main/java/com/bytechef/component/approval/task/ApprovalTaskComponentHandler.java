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

package com.bytechef.component.approval.task;

import static com.bytechef.component.approval.task.constant.ApprovalTaskConstants.APPROVAL_TASK;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.approval.task.action.ApproveAction;
import com.bytechef.component.approval.task.cluster.ApprovalTaskApprovalChannel;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(APPROVAL_TASK + "_v1_ComponentHandler")
public class ApprovalTaskComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public ApprovalTaskComponentHandler(ApprovalTaskService approvalTaskService) {
        this.componentDefinition = component(APPROVAL_TASK)
            .title("Approval Task")
            .description("Approval Task component for manual approval workflows.")
            .icon("path:assets/approval-task.svg")
            .categories(ComponentCategory.HELPERS)
            .actions(ApproveAction.ACTION_DEFINITION)
            .clusterElements(ApprovalTaskApprovalChannel.of(approvalTaskService));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
