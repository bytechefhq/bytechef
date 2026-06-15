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
 * Shared constants for the agent tool-suspend protocol: the sentinel a suspending tool returns as its result, and the
 * keys under which the resumable-loop state is stored in {@code Suspend.continueParameters}.
 *
 * @author Ivica Cardic
 */
public final class ToolSuspendConstants {

    /**
     * Returned by a tool as its result when it suspends. The {@code SuspendableToolCallingManager} locates the matching
     * {@code ToolResponse} by this exact value to identify the pending tool call.
     */
    public static final String SUSPENDED_SENTINEL = "__bytechef_tool_suspended__";

    /** {@code continueParameters} key holding the serialized {@code ConversationState}. */
    public static final String CONVERSATION_STATE = "__bytechef_conversation_state__";

    /** {@code continueParameters} key holding the pending tool call id (a {@code String}). */
    public static final String PENDING_TOOL_CALL_ID = "__bytechef_pending_tool_call_id__";

    private ToolSuspendConstants() {
    }
}
