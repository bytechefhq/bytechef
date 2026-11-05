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

package com.bytechef.component.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AgentChannelDefinition {

    String ATTACHMENTS = "attachments";
    String CONVERSATION_ID = "conversationId";
    String MESSAGE = "message";

    List<String> CONTRACT_PROPERTY_NAMES = List.of(ATTACHMENTS, CONVERSATION_ID, MESSAGE);

    String getName();

    Optional<String> getTitle();

    Optional<String> getDescription();

    @JsonIgnore
    TriggerDefinition getTrigger();

    default String getTriggerName() {
        TriggerDefinition triggerDefinition = getTrigger();

        return triggerDefinition.getName();
    }

    @JsonIgnore
    Optional<ActionDefinition> getReplyAction();

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    default @Nullable String getReplyActionName() {
        return getReplyAction()
            .map(ActionDefinition::getName)
            .orElse(null);
    }

    Optional<String> getApprovalChannelName();

    Map<String, Object> getTriggerParameters();
}
