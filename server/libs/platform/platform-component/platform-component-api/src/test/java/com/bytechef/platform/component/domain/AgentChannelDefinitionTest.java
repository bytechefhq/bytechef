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

package com.bytechef.platform.component.domain;

import static com.bytechef.component.definition.ComponentDsl.agentChannel;
import static com.bytechef.component.definition.ComponentDsl.agentChannelRequest;
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AgentChannelDefinitionTest {

    @Test
    void testTriggerParametersKeepDeclarationOrder() {
        List<String> declaredNames = List.of(
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");

        Map<String, Object> declaredTriggerParameters = new LinkedHashMap<>();

        for (String declaredName : declaredNames) {
            declaredTriggerParameters.put(declaredName, declaredName);
        }

        ModifiableTriggerDefinition triggerDefinition = trigger("newChatRequest")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(outputSchema(agentChannelRequest()))
            .agentRequest(agentRequest());

        AgentChannelDefinition agentChannelDefinition = new AgentChannelDefinition(
            agentChannel("chat", triggerDefinition).triggerParameters(declaredTriggerParameters), "chat", 1);

        Map<String, Object> triggerParameters = agentChannelDefinition.getTriggerParameters();

        assertThat(List.copyOf(triggerParameters.keySet())).isEqualTo(declaredNames);
    }
}
