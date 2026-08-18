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

package com.bytechef.ai.copilot.tool.ask;

/**
 * How {@link SubAgentAskRelayToolCallback} renders a question a specialist raised (see {@link SubAgentAskRelay}) when
 * it returns it as its own tool result.
 *
 * <p>
 * {@link #JSON} is every surface's default: the raw {@code ask-user-question} envelope, byte-identical to what the
 * panel/AI-Hub client's {@code toToolResultDataPart} already renders as a choice card. That contract is
 * client-load-bearing and must never change.
 * </p>
 *
 * <p>
 * {@link #PLAIN_TEXT} formats the same envelope as numbered, human-readable text via {@link SubAgentQuestionFormatter}
 * — for surfaces with no renderer of their own for the JSON envelope, such as an external MCP client.
 * </p>
 *
 * @author Ivica Cardic
 */
public enum SubAgentQuestionRenderer {
    JSON,
    PLAIN_TEXT
}
