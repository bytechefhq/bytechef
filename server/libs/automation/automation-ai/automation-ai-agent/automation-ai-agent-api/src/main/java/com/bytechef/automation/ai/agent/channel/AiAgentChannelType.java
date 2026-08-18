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

package com.bytechef.automation.ai.agent.channel;

/**
 * The three channel keys this module gives product semantics to. Every OTHER channel an {@code AiAgent} can be reached
 * through is discovered from the component registry (see {@code AgentChannelResolver} in
 * {@code automation-ai-agent-service}) and has no constant here — a Slack or Telegram channel is nothing but a row
 * whose {@code ai_agent_channel.channel_type} matches a component's own {@code agentChannel(...)} declaration.
 * <p>
 * The three below survive because code branches on them:
 * <ul>
 * <li>{@code CHAT} and {@code WORKFLOW_CALL} are ordinary discovered channels, but the facade auto-creates them with
 * every agent and refuses to delete them.</li>
 * <li>{@code SCHEDULE} is not a channel at all — it has nobody on the other end — and is the workflow generator's one
 * non-channel branch (see {@code AgentChannelResolver}'s javadoc).</li>
 * </ul>
 * Values are stored verbatim (STRING, not ordinal) in {@code ai_agent_channel.channel_type}.
 *
 * @author Ivica Cardic
 */
public final class AiAgentChannelType {

    public static final String CHAT = "chat";
    public static final String WORKFLOW_CALL = "workflowCall";
    public static final String SCHEDULE = "schedule";

    private AiAgentChannelType() {
    }
}
