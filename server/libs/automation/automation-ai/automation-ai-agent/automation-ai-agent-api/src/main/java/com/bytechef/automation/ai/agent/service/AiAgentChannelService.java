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

package com.bytechef.automation.ai.agent.service;

import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface AiAgentChannelService {

    AiAgentChannel create(AiAgentChannel agentChannel);

    void delete(long id);

    AiAgentChannel getAgentChannel(long id);

    List<AiAgentChannel> getByAgentId(long agentId);

    AiAgentChannel update(AiAgentChannel agentChannel);
}
