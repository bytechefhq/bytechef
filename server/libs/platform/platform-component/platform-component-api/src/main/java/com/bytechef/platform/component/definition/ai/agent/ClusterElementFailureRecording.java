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

package com.bytechef.platform.component.definition.ai.agent;

import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord.Status;

/**
 * Records a failure on the cluster element that raised it, once per exception.
 *
 * @author Ivica Cardic
 */
final class ClusterElementFailureRecording {

    private ClusterElementFailureRecording() {
    }

    static void record(
        ClusterElementCallTarget target, String workflowNodeName, String componentName, Throwable throwable) {

        if (!target.recorder()
            .claimFailure(target.taskExecutionId(), throwable)) {

            return;
        }

        ClusterElementCallRef parent = ClusterElementCallScope.current();
        String message = throwable.getMessage();

        target.recorder()
            .recordOutcome(
                target.jobId(), target.taskExecutionId(), workflowNodeName, componentName,
                parent == null ? null : parent.id(), Status.FAILED, null,
                message == null ? throwable.getClass()
                    .getSimpleName() : message);
    }
}
