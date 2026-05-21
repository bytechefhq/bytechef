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

package com.bytechef.platform.workflow.task.dispatcher.subflow;

/**
 * Keys for the agent-tool durable sub-workflow bridge.
 *
 * @author Ivica Cardic
 */
public final class SubflowRequestConstants {

    /** Key in the agent's {@code Suspend.continueParameters} holding a {@link PendingSubflowRequest}. */
    public static final String PENDING_SUBFLOW = "__bytechef_pending_subflow__";

    /** Key in the sub-workflow job's metadata holding the agent job id (a {@code Long}). */
    public static final String AGENT_JOB_ID = "__bytechef_agent_job_id__";

    /** Key in the agent job's metadata holding the launched sub-workflow job id (idempotency guard). */
    public static final String LAUNCHED_SUBFLOW_JOB_ID = "__bytechef_launched_subflow_job_id__";

    private SubflowRequestConstants() {
    }
}
