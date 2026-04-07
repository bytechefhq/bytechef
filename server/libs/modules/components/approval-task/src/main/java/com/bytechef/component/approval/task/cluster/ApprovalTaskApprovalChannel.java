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

import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;

/**
 * @author Ivica Cardic
 */
public class ApprovalTaskApprovalChannel {

    public static ClusterElementDefinition<ApprovalChannelFunction> of(ApprovalTaskService approvalTaskService) {
        return ComponentDsl.<ApprovalChannelFunction>clusterElement("approvalTask")
            .title("Approval Task")
            .description("Sends an approval request via the Approval Task channel.")
            .type(APPROVAL_CHANNELS)
            .object(
                () -> (inputParameters, connectionParameters, formUrl, context) -> perform(
                    inputParameters, connectionParameters, formUrl, context, approvalTaskService));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, String formUrl,
        ClusterElementContext context, ApprovalTaskService approvalTaskService) {

        return null;
    }
}
