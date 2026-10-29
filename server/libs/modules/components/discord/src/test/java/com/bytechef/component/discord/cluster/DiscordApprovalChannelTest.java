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

package com.bytechef.component.discord.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class DiscordApprovalChannelTest {

    @Test
    void testEscapeMarkdownNeutralizesLinkSyntax() {
        assertThat(DiscordApprovalChannel.escapeMarkdown("[Approve here](https://evil.example)"))
            .isEqualTo("\\[Approve here\\]\\(https://evil.example\\)");
    }

    @Test
    void testEscapeMarkdownEscapesBackslashBeforeBrackets() {
        assertThat(DiscordApprovalChannel.escapeMarkdown("a\\b")).isEqualTo("a\\\\b");
    }

    @Test
    void testEscapeMarkdownLeavesBenignTextUnchanged() {
        assertThat(DiscordApprovalChannel.escapeMarkdown("Approve the expense report"))
            .isEqualTo("Approve the expense report");
    }

    @Nested
    class Perform {

        private static final String FORM_URL = "https://example.com/approval/abc";

        private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
        private final ClusterElementContext context = mock(ClusterElementContext.class);

        @BeforeEach
        void setUp() {
            Http.Executor executor = mock(Http.Executor.class);
            Http.Response response = mock(Http.Response.class);

            when(context.http(any())).thenReturn(executor);
            when(executor.body(bodyArgumentCaptor.capture())).thenReturn(executor);
            when(executor.configuration(any())).thenReturn(executor);
            when(executor.execute()).thenReturn(response);
            when(response.getBody(any(TypeReference.class))).thenReturn(Map.of());
        }

        @Test
        void testShortResumeIdIsEncodedIntoInteractionButtons() throws Exception {
            perform("short-1");

            assertThat(buttons()).extracting(button -> button.get("custom_id"))
                .containsExactly("short-1:a", "short-1:d");
        }

        @Test
        void testMissingShortResumeIdFallsBackToFormLinks() throws Exception {
            perform(null);

            assertThat(buttons()).extracting(button -> button.get("url"))
                .containsExactly(FORM_URL + "?approved=true", FORM_URL + "?approved=false");
        }

        private void perform(String shortResumeId) throws Exception {
            ApprovalChannelFunction approvalChannelFunction = DiscordApprovalChannel.CLUSTER_ELEMENT_DEFINITION
                .getElement();

            Map<String, Object> inputParameters = new HashMap<>(
                Map.of(
                    "channelId", "c-1", "inputs", List.of(), ApprovalChannelFunction.PUBLIC_URL, "https://example.com",
                    ApprovalChannelFunction.RESUME_ID, "resume-token"));

            if (shortResumeId != null) {
                inputParameters.put(ApprovalChannelFunction.SHORT_RESUME_ID, shortResumeId);
            }

            approvalChannelFunction.apply(
                MockParametersFactory.create(inputParameters),
                MockParametersFactory.create(Map.of("publicKey", "abcdef")), FORM_URL, context);
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> buttons() {
            Body body = bodyArgumentCaptor.getValue();

            Map<String, ?> content = (Map<String, ?>) body.getContent();
            List<Map<String, ?>> rows = (List<Map<String, ?>>) content.get("components");

            return (List<Map<String, Object>>) rows.getFirst()
                .get("components");
        }
    }
}
