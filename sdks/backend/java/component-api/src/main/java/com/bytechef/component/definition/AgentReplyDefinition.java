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

import java.util.Map;
import java.util.Optional;

/**
 * Marks an action as the reply half of an agent channel and states which of that action's own properties receive the
 * agent channel contract fields. A property name may be dotted, addressing a member of a declared dynamic-properties
 * map ({@code "response.message"}); only its first segment is a declared property of the action.
 *
 * @author Ivica Cardic
 */
public interface AgentReplyDefinition {

    /**
     * Returns the action property that receives the agent's reply text.
     *
     * @return the message property name, defaulting to {@link AgentChannelDefinition#MESSAGE}
     */
    String getMessageProperty();

    /**
     * Returns the action property that receives the conversation identifier the reply is addressed to.
     *
     * @return an {@link Optional} containing the conversation identifier property, or an empty {@link Optional} when
     *         the reply is not addressed (a synchronous webhook or subflow response)
     */
    Optional<String> getConversationIdProperty();

    /**
     * Returns the action property that receives the reply attachments.
     *
     * @return an {@link Optional} containing the attachments property, or an empty {@link Optional} when the channel
     *         carries no attachments
     */
    Optional<String> getAttachmentsProperty();

    /**
     * Returns the properties this action takes from the channel row's own parameters, mapping each channel-row
     * parameter name to the action property it fills. A row key must also name a property of the paired trigger, so
     * that one channel row configures both ends of the channel (Twilio's {@code "number"}).
     *
     * @return the channel-row parameter to action property mapping, or an empty map if none are defined
     */
    Map<String, String> getChannelParameters();

    /**
     * Returns the values always sent with the reply, mapping each action property to its fixed value (Twilio's
     * {@code useTemplate = false}).
     *
     * @return the fixed action property values, or an empty map if none are defined
     */
    Map<String, Object> getFixedParameters();
}
