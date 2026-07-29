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

package com.bytechef.platform.ai.a2a;

import org.jspecify.annotations.Nullable;

/**
 * The resolved input passed to an {@link A2AAgentExecutor}: the identity of the addressed agent and the user's text
 * extracted from the inbound A2A message, plus optional conversation continuity ids.
 *
 * @param agentId   identifier of the addressed agent (implementation-defined; e.g. an agent-backed workflow reference)
 * @param text      the user text extracted from the inbound A2A message parts
 * @param contextId the A2A context id for multi-turn continuity, or {@code null} for a new conversation
 * @param messageId the inbound A2A message id, or {@code null} if none was supplied
 * @author Ivica Cardic
 */
public record A2AAgentRequest(
    String agentId, String text, @Nullable String contextId, @Nullable String messageId) {
}
