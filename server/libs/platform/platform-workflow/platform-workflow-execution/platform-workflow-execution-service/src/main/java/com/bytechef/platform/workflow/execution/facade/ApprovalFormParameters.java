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

package com.bytechef.platform.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the parameters an approval form is built from: those the suspend carries, which an approval tool sets because
 * its suspended task is the agent's, otherwise the suspended task's own.
 *
 * @author Ivica Cardic
 */
final class ApprovalFormParameters {

    private ApprovalFormParameters() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, ?> get(TaskStateService taskStateService, JobResumeId jobResumeId, TaskExecution taskExecution) {
        Optional<Suspend> suspendOptional = taskStateService.fetchValue(jobResumeId);

        if (suspendOptional.isPresent()) {
            Suspend suspend = suspendOptional.get();

            if (suspend.continueParameters()
                .get(MetadataConstants.APPROVAL_FORM_PARAMETERS) instanceof Map<?, ?> formParameters) {

                return (Map<String, ?>) formParameters;
            }
        }

        Map<String, ?> parameters = taskExecution.getParameters();

        return parameters == null ? Map.of() : parameters;
    }
}
