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

package com.bytechef.platform.workflow.test.facade;

/**
 * Wraps the result of an AI agent action execution along with token usage metadata.
 *
 * @param output           the action execution result
 * @param promptTokens     the number of tokens used in the prompt (input)
 * @param completionTokens the number of tokens used in the completion (output)
 * @author Ivica Cardic
 */
public record AiAgentTestResult(Object output, int promptTokens, int completionTokens) {
}
