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

package com.bytechef.component.approval.action;

import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Deprecated
public class ApprovalLinkCreateApprovalLinksAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createApprovalLinks")
        .title("Create Approval Links")
        .description("Creates approval/disapproval links.")
        .properties()
        .output(ApprovalLinkCreateApprovalLinksAction::output)
        .perform(ApprovalLinkCreateApprovalLinksAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.approval(ActionContext.Approval::generateLinks);
    }

    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return OutputResponse.of(
            Map.of(
                "approvalLink", "https://example.com/approvals/s244aeoqwed853/jobs/12/true",
                "disapprovalLink", "https://example.com/approvals/s244aeoqwed853/jobs/12/false"));
    }
}
