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

package com.bytechef.component.approval.task.cluster;

import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.facade.ApprovalTaskFacade;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;

/**
 * @author Ivica Cardic
 */
public class ApprovalTaskApprovalChannel {

    public static ClusterElementDefinition<ApprovalChannelFunction> of(ApprovalTaskFacade approvalTaskFacade) {
        return ComponentDsl.<ApprovalChannelFunction>clusterElement("approvalTask")
            .title("Approval Task")
            .description("Sends an approval request via the Approval Task channel.")
            .type(APPROVAL_CHANNELS)
            .object(
                () -> (inputParameters, connectionParameters, formUrl, context) -> perform(
                    inputParameters, formUrl, approvalTaskFacade));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static Object perform(Parameters inputParameters, String formUrl, ApprovalTaskFacade approvalTaskFacade) {
        String jobResumeId = formUrl.substring(formUrl.lastIndexOf('/') + 1);

        ApprovalTask approvalTask = ApprovalTask.builder()
            .name(inputParameters.getString("formTitle"))
            .description(inputParameters.getString("formDescription"))
            .jobResumeId(jobResumeId)
            .status(ApprovalTask.Status.OPEN)
            .priority(ApprovalTask.Priority.MEDIUM)
            .build();

        approvalTaskFacade.createApprovalTask(approvalTask);

        return null;
    }
}
