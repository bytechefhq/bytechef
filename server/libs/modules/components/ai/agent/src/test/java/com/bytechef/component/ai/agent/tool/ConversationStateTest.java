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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ConversationStateTest {

    private static byte[] createImageBytes() {
        return new byte[] {
            (byte) 0x89, 'P', 'N', 'G', 0, 1, 2, (byte) 0xFF
        };
    }

    private static final String IMAGE_URL = "https://example.com/image.png";
    private static final byte[] THOUGHT_PART_1 = "signature-one".getBytes(StandardCharsets.UTF_8);
    private static final byte[] THOUGHT_PART_2 = {
        0, (byte) 0xFE, 42
    };

    @Test
    void testJacksonRoundTripPreservesAllEntryKinds() {
        ConversationState conversationState = ConversationState.from(
            List.of(
                new SystemMessage("you are an agent"),
                new UserMessage("please get approval"),
                AssistantMessage.builder()
                    .content("")
                    .toolCalls(
                        List.of(new AssistantMessage.ToolCall("call_1", "function", "requestApproval", "{\"a\":1}")))
                    .build(),
                ToolResponseMessage.builder()
                    .responses(
                        List.of(new ToolResponseMessage.ToolResponse("call_1", "requestApproval", "pending")))
                    .build()));

        String json = JsonUtils.write(conversationState);

        assertThat(json).contains(
            "\"kind\":\"system\"", "\"kind\":\"user\"", "\"kind\":\"assistant\"", "\"kind\":\"tool\"");

        ConversationState restoredConversationState = JsonUtils.read(json, ConversationState.class);

        assertThat(restoredConversationState).isEqualTo(conversationState);

        List<Message> messages = restoredConversationState.toMessages();

        assertThat(messages).hasSize(4);

        SystemMessage systemMessage = (SystemMessage) messages.get(0);

        assertThat(systemMessage.getText()).isEqualTo("you are an agent");

        UserMessage userMessage = (UserMessage) messages.get(1);

        assertThat(userMessage.getText()).isEqualTo("please get approval");

        AssistantMessage assistantMessage = (AssistantMessage) messages.get(2);

        assertThat(assistantMessage.getToolCalls()).containsExactly(
            new AssistantMessage.ToolCall("call_1", "function", "requestApproval", "{\"a\":1}"));

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) messages.get(3);

        assertThat(toolResponseMessage.getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse("call_1", "requestApproval", "pending"));
    }

    @Test
    void testJacksonRoundTripPreservesUserMedia() {
        UserMessage originalUserMessage = UserMessage.builder()
            .text("what is in these images?")
            .media(
                Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_PNG)
                    .data(createImageBytes())
                    .id("image_1")
                    .name("inline.png")
                    .build(),
                Media.builder()
                    .mimeType(MimeTypeUtils.IMAGE_JPEG)
                    .data(IMAGE_URL)
                    .name("remote.jpg")
                    .build())
            .build();

        List<Message> messages = roundTrip(List.of(originalUserMessage));

        UserMessage userMessage = (UserMessage) messages.getFirst();

        assertThat(userMessage.getText()).isEqualTo("what is in these images?");

        List<Media> media = userMessage.getMedia();

        assertThat(media).hasSize(2);

        Media inlineMedia = media.get(0);

        assertThat(inlineMedia.getMimeType()).isEqualTo(MimeTypeUtils.IMAGE_PNG);
        assertThat(inlineMedia.getDataAsByteArray()).containsExactly(createImageBytes());
        assertThat(inlineMedia.getId()).isEqualTo("image_1");
        assertThat(inlineMedia.getName()).isEqualTo("inline.png");

        Media urlMedia = media.get(1);

        assertThat(urlMedia.getMimeType()).isEqualTo(MimeTypeUtils.IMAGE_JPEG);
        assertThat(urlMedia.getData()).isEqualTo(IMAGE_URL);
        assertThat(urlMedia.getName()).isEqualTo("remote.jpg");
    }

    @Test
    void testJacksonRoundTripPreservesAssistantMetadataIncludingByteArrays() {
        Map<String, Object> properties = new HashMap<>();

        properties.put("thoughtSignatures", List.of(THOUGHT_PART_1, THOUGHT_PART_2));
        properties.put("finishReason", "TOOL_CALLS");
        properties.put("index", 3);
        properties.put("nested", Map.of("flag", true));
        properties.put("notJson", new Object());

        AssistantMessage originalAssistantMessage = AssistantMessage.builder()
            .content("thinking")
            .toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", "requestApproval", "{}")))
            .properties(properties)
            .build();

        List<Message> messages = roundTrip(List.of(originalAssistantMessage));

        AssistantMessage assistantMessage = (AssistantMessage) messages.getFirst();

        Map<String, Object> metadata = assistantMessage.getMetadata();

        assertThat(metadata).containsEntry("finishReason", "TOOL_CALLS")
            .containsEntry("index", 3)
            .containsEntry("nested", Map.of("flag", true))
            .doesNotContainKey("notJson");

        List<?> thoughtSignatures = (List<?>) metadata.get("thoughtSignatures");

        assertThat(thoughtSignatures).hasSize(2);
        assertThat(thoughtSignatures.get(0)).isInstanceOf(byte[].class);
        assertThat((byte[]) thoughtSignatures.get(0)).containsExactly(THOUGHT_PART_1);
        assertThat((byte[]) thoughtSignatures.get(1)).containsExactly(THOUGHT_PART_2);
    }

    @Test
    void testMessageTypeMetadataIsNotPersisted() {
        ConversationState conversationState = ConversationState.from(List.of(new UserMessage("hello")));

        ConversationState.UserEntry userEntry = (ConversationState.UserEntry) conversationState.messages()
            .getFirst();

        assertThat(userEntry.metadata()).doesNotContainKey(AbstractMessage.MESSAGE_TYPE);
    }

    @Test
    void testNullListsAndMapsBecomeEmpty() {
        assertThat(new ConversationState(null).messages()).isEmpty();

        ConversationState.SystemEntry systemEntry = new ConversationState.SystemEntry("system", null);

        assertThat(systemEntry.metadata()).isEmpty();

        ConversationState.UserEntry userEntry = new ConversationState.UserEntry("user", null, null);

        assertThat(userEntry.media()).isEmpty();
        assertThat(userEntry.metadata()).isEmpty();

        ConversationState.AssistantEntry assistantEntry = new ConversationState.AssistantEntry(null, null, null, null);

        assertThat(assistantEntry.toolCalls()).isEmpty();
        assertThat(assistantEntry.media()).isEmpty();
        assertThat(assistantEntry.metadata()).isEmpty();

        ConversationState.ToolEntry toolEntry = new ConversationState.ToolEntry(null, null);

        assertThat(toolEntry.toolResponses()).isEmpty();
        assertThat(toolEntry.metadata()).isEmpty();

        List<Message> messages = new ConversationState(
            List.of(systemEntry, userEntry, assistantEntry, toolEntry)).toMessages();

        assertThat(messages).hasSize(4);
    }

    @Test
    void testDeserializesPayloadWithMissingAndNullFields() {
        String json = """
            {"messages":[
              {"kind":"system","text":"system"},
              {"kind":"user","text":"hi","media":null},
              {"kind":"assistant","text":null,"toolCalls":null,"metadata":null},
              {"kind":"tool"}
            ]}
            """;

        ConversationState conversationState = JsonUtils.read(json, ConversationState.class);

        assertThat(conversationState.messages()).hasSize(4);

        List<Message> messages = conversationState.toMessages();

        assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(((UserMessage) messages.get(1)).getMedia()).isEmpty();
        assertThat(((AssistantMessage) messages.get(2)).getToolCalls()).isEmpty();
        assertThat(((ToolResponseMessage) messages.get(3)).getResponses()).isEmpty();
    }

    @Test
    void testDeserializesTheOldToolEntryShape() {
        String json = """
            {"messages":[
              {"kind":"tool","text":"","toolCalls":[],
               "toolResponses":[{"id":"call_1","name":"requestApproval","responseData":"pending"}]}
            ]}
            """;

        ConversationState conversationState = JsonUtils.read(json, ConversationState.class);

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) conversationState.toMessages()
            .getFirst();

        assertThat(toolResponseMessage.getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse("call_1", "requestApproval", "pending"));
    }

    @Test
    void testStateStoredWithoutAVersionReadsAsTheCurrentVersion() {
        ConversationState conversationState = JsonUtils.read(
            """
                {"messages":[{"kind":"system","text":"system"}],"unknownField":true}
                """,
            ConversationState.class);

        assertThat(conversationState.version()).isEqualTo(ConversationState.CURRENT_VERSION);
        assertThat(conversationState.messages()).hasSize(1);
    }

    @Test
    void testStateWithANewerVersionIsRejected() {
        int newerVersion = ConversationState.CURRENT_VERSION + 1;

        assertThatThrownBy(() -> new ConversationState(newerVersion, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("version " + newerVersion);
    }

    @Test
    void testSystemAndUserEntriesWithoutTextGetEmptyText() {
        assertThat(new ConversationState.SystemEntry(null, null).text()).isEmpty();
        assertThat(new ConversationState.UserEntry(null, null, null).text()).isEmpty();

        ConversationState conversationState = JsonUtils.read(
            """
                {"messages":[{"kind":"system"},{"kind":"user"}]}
                """,
            ConversationState.class);

        List<Message> messages = conversationState.toMessages();

        assertThat(messages.get(0)
            .getText()).isEmpty();
        assertThat(messages.get(1)
            .getText()).isEmpty();
    }

    @Test
    void testMetadataMapThatLooksLikeTheOldBytesMarkerStaysAMap() {
        AssistantMessage originalAssistantMessage = AssistantMessage.builder()
            .content("answer")
            .properties(Map.of("signature", Map.of("$bytes", "AAAA")))
            .build();

        AssistantMessage assistantMessage = (AssistantMessage) roundTrip(List.of(originalAssistantMessage)).getFirst();

        assertThat(assistantMessage.getMetadata()).containsEntry("signature", Map.of("$bytes", "AAAA"));
    }

    @Test
    void testWrittenStateCarriesTheCurrentVersion() {
        String json = JsonUtils.write(ConversationState.from(List.of(new UserMessage("hi"))));

        assertThat(json).contains("\"version\":" + ConversationState.CURRENT_VERSION);
    }

    @Test
    void testMediaEntryRequiresExactlyOneOfUrlAndData() {
        assertThatThrownBy(() -> new ConversationState.MediaEntry("image/png", null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConversationState.MediaEntry("image/png", null, null, "https://x", "AAAA"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConversationState.MediaEntry(null, null, null, "https://x", null))
            .isInstanceOf(NullPointerException.class);
    }

    private static List<Message> roundTrip(List<Message> messages) {
        String json = JsonUtils.write(ConversationState.from(messages));

        ConversationState conversationState = JsonUtils.read(json, ConversationState.class);

        return conversationState.toMessages();
    }
}
