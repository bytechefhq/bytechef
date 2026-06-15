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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

class ConversationResumeTest {

    @Test
    void testPatchReplacesOnlyTheSentinelResponse() {
        List<Message> conversation = List.of(
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse("call_a", "otherTool", "kept"),
                        new ToolResponseMessage.ToolResponse(
                            "call_b", "requestApproval", ToolSuspendConstants.SUSPENDED_SENTINEL)))
                .build());

        List<Message> patched = ConversationResume.patchPendingToolResponse(
            conversation, "call_b", "{\"approved\":true}");

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) patched.get(0);

        assertEquals("kept", toolResponseMessage.getResponses()
            .get(0)
            .responseData());
        assertEquals(
            "{\"approved\":true}",
            toolResponseMessage.getResponses()
                .get(1)
                .responseData());
    }

    @Test
    void testPatchThrowsWhenPendingIdMatchesNothing() {
        List<Message> conversation = List.of(
            new SystemMessage("you are a helper"),
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse("call_a", "otherTool", "kept")))
                .build());

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> ConversationResume.patchPendingToolResponse(conversation, "missing_id", "{\"answer\":\"yes\"}"));

        assertTrue(
            exception.getMessage()
                .contains("missing_id"),
            "Exception message should reference the unmatched id");
        assertTrue(
            exception.getMessage()
                .contains("0"),
            "Exception message should report the patched count (0)");
    }

    @Test
    void testPatchThrowsWhenMultipleResponsesMatchTheSameId() {
        // Defensive: if a future bug ever produces two responses with the same id, fail loud rather than patch both
        // and silently corrupt the second one.
        List<Message> conversation = List.of(
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse(
                            "call_dup", "requestApproval", ToolSuspendConstants.SUSPENDED_SENTINEL),
                        new ToolResponseMessage.ToolResponse(
                            "call_dup", "requestApproval", ToolSuspendConstants.SUSPENDED_SENTINEL)))
                .build());

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> ConversationResume.patchPendingToolResponse(conversation, "call_dup", "{\"answer\":\"yes\"}"));

        assertTrue(
            exception.getMessage()
                .contains("call_dup"));
        assertTrue(
            exception.getMessage()
                .contains("2"));
    }
}
