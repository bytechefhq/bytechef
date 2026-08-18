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
 * Pairs a request trigger with an optional reply action on one component, declaring the way an AI agent is reached
 * through that component. The trigger carries an {@link AgentRequestDefinition} stating where the contract fields live
 * in its output; the reply action, when there is one, carries an {@link AgentReplyDefinition} stating which of its
 * properties receive them.
 *
 * @author Ivica Cardic
 */
public interface AgentChannelDefinition {

    String ATTACHMENTS = "attachments";
    String CONVERSATION_ID = "conversationId";
    String MESSAGE = "message";

    List<String> CONTRACT_PROPERTY_NAMES = List.of(ATTACHMENTS, CONVERSATION_ID, MESSAGE);

    /**
     * Returns the channel key, stored verbatim as the channel type of an agent's channel row and used as the generated
     * trigger node-name prefix. Unique across components by convention, enforced when components are loaded.
     *
     * @return the channel name
     */
    String getName();

    /**
     * Returns the human-readable title of this channel displayed in the user interface.
     *
     * @return an {@link Optional} containing the title, or an empty {@link Optional} if none is set
     */
    Optional<String> getTitle();

    /**
     * Returns the human-readable description of this channel displayed in the user interface.
     *
     * @return an {@link Optional} containing the description, or an empty {@link Optional} if none is set
     */
    Optional<String> getDescription();

    /**
     * Returns the trigger through which a request reaches the agent on this channel.
     * <p>
     * Never serialized: the trigger is already present verbatim under the component's own {@code triggers}, and
     * embedding a second complete copy per channel made component definitions unreadable. {@link #getTriggerName()} is
     * serialized in its place, which is what a reader of the definition needs. Nothing reads this back from JSON —
     * every consumer holds the definition in process.
     *
     * @return the request trigger
     */
    @JsonIgnore
    TriggerDefinition getTrigger();

    /**
     * Returns the name of the trigger through which a request reaches the agent on this channel.
     *
     * @return the request trigger name
     */
    default String getTriggerName() {
        TriggerDefinition triggerDefinition = getTrigger();

        return triggerDefinition.getName();
    }

    /**
     * Returns the action through which the agent's reply leaves on this channel.
     * <p>
     * Never serialized, for the same reason as {@link #getTrigger()}; {@link #getReplyActionName()} is serialized in
     * its place.
     *
     * @return an {@link Optional} containing the reply action, or an empty {@link Optional} when the channel has nobody
     *         to answer
     */
    @JsonIgnore
    Optional<ActionDefinition> getReplyAction();

    /**
     * Returns the name of the action through which the agent's reply leaves on this channel.
     * <p>
     * Absent from the serialized definition when the channel has nobody to answer.
     *
     * @return the reply action name, or {@code null} when the channel has no reply action
     */
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    default @Nullable String getReplyActionName() {
        return getReplyAction()
            .map(ActionDefinition::getName)
            .orElse(null);
    }

    /**
     * Returns the name of the approval-channel cluster element, on the same component, through which human approvals
     * reach the user on this channel.
     *
     * @return an {@link Optional} containing the approval channel name, or an empty {@link Optional} when the channel
     *         cannot carry an approval
     */
    Optional<String> getApprovalChannelName();

    /**
     * Returns the trigger parameters that are always emitted for this channel, distinct from a trigger property's own
     * default value.
     *
     * @return the fixed trigger parameters, or an empty map if none are defined
     */
    Map<String, Object> getTriggerParameters();
}
