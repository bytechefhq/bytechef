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

package com.bytechef.platform.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.ai.tool.GatedToolSuspension.ApprovalRequest;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class GatedToolSuspensionTest {

    @Test
    void testRoundTripsThroughThePersistedContinueParameterKeys() {
        GatedToolSuspension gatedToolSuspension = new GatedToolSuspension("SLACK_SEND_MESSAGE", "{\"text\": \"hi\"}");

        Map<String, Object> continueParameters = gatedToolSuspension.toContinueParameters();

        assertThat(continueParameters)
            .containsEntry("__bytechef_gated_tool_name__", "SLACK_SEND_MESSAGE")
            .containsEntry("__bytechef_gated_tool_input__", "{\"text\": \"hi\"}");
        assertThat(GatedToolSuspension.from(continueParameters)).contains(gatedToolSuspension);
    }

    @Test
    void testApprovalRequestRoundTripsThroughTheWireKeys() {
        Instant raisedAt = Instant.parse("2026-10-17T10:15:30Z");

        GatedToolSuspension gatedToolSuspension = new GatedToolSuspension(
            "SLACK_SEND_MESSAGE", "{}",
            new ApprovalRequest("Approve tool call", "The agent wants to call", "https://example.com/form", raisedAt));

        Map<String, Object> continueParameters = gatedToolSuspension.toContinueParameters();

        assertThat(continueParameters)
            .containsEntry("formUrl", "https://example.com/form")
            .containsEntry("approvalRaisedAt", "2026-10-17T10:15:30Z")
            .containsEntry("__bytechef_approval_resume__", true)
            .containsEntry(
                "approvalFormParameters",
                Map.of("formTitle", "Approve tool call", "formDescription", "The agent wants to call"));
        assertThat(GatedToolSuspension.from(continueParameters)).contains(gatedToolSuspension);
    }

    @Test
    void testNonGatedSuspendIsEmpty() {
        assertThat(GatedToolSuspension.from(Map.of("formUrl", "https://example.com"))).isEmpty();
    }

    @Test
    void testGatedSuspendWithoutInputIsRejected() {
        assertThatThrownBy(() -> GatedToolSuspension.from(Map.of("__bytechef_gated_tool_name__", "SLACK")))
            .isInstanceOf(IllegalStateException.class);
    }
}
