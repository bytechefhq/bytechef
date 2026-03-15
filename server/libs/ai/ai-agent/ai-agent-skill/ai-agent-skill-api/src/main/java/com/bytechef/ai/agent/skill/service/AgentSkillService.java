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

package com.bytechef.ai.agent.skill.service;

import com.bytechef.ai.agent.skill.domain.AgentSkill;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AgentSkillService {

    AgentSkill createAgentSkill(AgentSkill agentSkill);

    void deleteAgentSkill(long id);

    boolean existsByName(String name);

    AgentSkill getAgentSkill(long id);

    List<AgentSkill> getAgentSkills();

    AgentSkill updateAgentSkill(long id, String name, @Nullable String description);
}
