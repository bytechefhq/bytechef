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

package com.bytechef.platform.ai.constant;

/**
 * Constants for SSE event types used in AI agent streaming responses.
 *
 * @author Ivica Cardic
 */
public final class AiAgentSseEventType {

    private AiAgentSseEventType() {
    }

    public static final String EVENT_TYPE = "__eventType";

    public static final String ASK_USER_QUESTION = "ask_user_question";
    public static final String TOOL_EXECUTION = "tool_execution";
}
