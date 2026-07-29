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

package com.bytechef.component.ai.agent.tool;

/**
 * Durable checkpoint of an in-flight agent conversation, written to execution-scoped data storage after every completed
 * tool round so a crash mid-loop can resume from the last completed tool call instead of re-running the whole turn. The
 * {@code fingerprint} identifies the agent node's evaluated input parameters: on resume the checkpoint is honored only
 * when the fingerprint matches, so a checkpoint written by a different agent node in the same job (or by an earlier,
 * differently-parameterized run) is ignored rather than wrongly replayed.
 *
 * @author Ivica Cardic
 */
public record AiAgentConversationCheckpoint(String fingerprint, ConversationState conversationState) {

    public static final String DATA_KEY = "aiAgentConversationCheckpoint";
}
