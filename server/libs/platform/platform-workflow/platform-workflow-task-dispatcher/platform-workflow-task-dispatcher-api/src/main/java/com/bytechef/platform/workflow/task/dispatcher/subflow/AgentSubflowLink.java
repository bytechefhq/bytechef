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

import com.bytechef.commons.util.MapUtils;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;

/**
 * Links a sub-workflow job to the suspended agent job that launched it and waits on its result.
 *
 * @author Ivica Cardic
 */
public record AgentSubflowLink(long agentJobId, String agentJobResumeId) {

    public AgentSubflowLink {
        Validate.notBlank(agentJobResumeId, "agentJobResumeId");
    }

    public static Optional<AgentSubflowLink> fromMetadata(Map<String, ?> metadata) {
        Long agentJobId = MapUtils.getLong(metadata, SubflowRequestConstants.AGENT_JOB_ID);
        String agentJobResumeId = MapUtils.getString(metadata, SubflowRequestConstants.AGENT_JOB_RESUME_ID);

        if (agentJobId == null || agentJobResumeId == null) {
            return Optional.empty();
        }

        return Optional.of(new AgentSubflowLink(agentJobId, agentJobResumeId));
    }

    public Map<String, Object> toMetadata() {
        return Map.of(
            SubflowRequestConstants.AGENT_JOB_ID, agentJobId, SubflowRequestConstants.AGENT_JOB_RESUME_ID,
            agentJobResumeId);
    }
}
