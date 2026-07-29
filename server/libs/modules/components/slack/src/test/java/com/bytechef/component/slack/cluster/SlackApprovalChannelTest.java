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

package com.bytechef.component.slack.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SlackApprovalChannelTest {

    @Test
    void testEscapeMrkdwnNeutralizesLinkSyntax() {
        // A gated tool's description embeds AI-chosen arguments verbatim; without escaping, mrkdwn angle brackets could
        // forge a clickable <url|text> link next to the real Approve/Discard buttons.
        assertThat(SlackApprovalChannel.escapeMrkdwn("<https://evil.example|Approve here>"))
            .isEqualTo("&lt;https://evil.example|Approve here&gt;")
            .doesNotContain("<")
            .doesNotContain(">");
    }

    @Test
    void testEscapeMrkdwnEscapesAmpersandBeforeAngleBrackets() {
        assertThat(SlackApprovalChannel.escapeMrkdwn("a & b < c > d")).isEqualTo("a &amp; b &lt; c &gt; d");
    }

    @Test
    void testEscapeMrkdwnLeavesBenignTextUnchanged() {
        assertThat(SlackApprovalChannel.escapeMrkdwn("Approve the expense report"))
            .isEqualTo("Approve the expense report");
    }
}
