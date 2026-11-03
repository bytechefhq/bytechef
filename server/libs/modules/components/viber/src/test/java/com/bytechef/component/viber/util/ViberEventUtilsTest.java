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

package com.bytechef.component.viber.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockContextSetupExtension.class, ObjectMapperSetupExtension.class
})
class ViberEventUtilsTest {

    private static final Set<String> ALL_EVENTS =
        Set.of("message", "subscribed", "unsubscribed", "conversation_started");

    @Test
    void testToOutputDropsWebhookHandshake(TriggerContext mockedContext) {
        List<Map<String, Object>> output = ViberEventUtils.toOutput(
            Map.of("event", "webhook", "timestamp", 1L), ALL_EVENTS, mockedContext);

        assertTrue(output.isEmpty());
    }

    @Test
    void testToOutputDropsUnselectedEvent(TriggerContext mockedContext) {
        List<Map<String, Object>> output = ViberEventUtils.toOutput(
            Map.of("event", "unsubscribed", "user_id", "u1"), Set.of("message"), mockedContext);

        assertTrue(output.isEmpty());
    }

    @Test
    void testToOutputMapsTextMessage(TriggerContext mockedContext) {
        Map<String, Object> event = Map.of(
            "event", "message", "timestamp", 1457764197627L, "message_token", 4912661846655238145L,
            "sender", Map.of("id", "01234567890A=", "name", "John"),
            "message", Map.of("type", "text", "text", "Hello"));

        List<Map<String, Object>> output = ViberEventUtils.toOutput(event, ALL_EVENTS, mockedContext);

        assertEquals(1, output.size());

        Map<String, Object> result = output.getFirst();

        assertEquals("message", result.get("event"));
        assertEquals(1457764197627L, result.get("timestamp"));
        assertEquals(4912661846655238145L, result.get("message_token"));
        assertEquals(Map.of("id", "01234567890A=", "name", "John"), result.get("user"));
        assertEquals(Map.of("type", "text", "text", "Hello"), result.get("message"));
        assertFalse(result.containsKey("file"));
    }

    @Test
    void testToOutputMapsUnsubscribedUserId(TriggerContext mockedContext) {
        Map<String, Object> event = Map.of("event", "unsubscribed", "timestamp", 5L, "user_id", "u1");

        Map<String, Object> result = ViberEventUtils.toOutput(event, ALL_EVENTS, mockedContext)
            .getFirst();

        assertEquals(Map.of("id", "u1"), result.get("user"));
        assertEquals("unsubscribed", result.get("event"));
        assertFalse(result.containsKey("message"));
    }

    @Test
    void testToOutputKeepsConversationStartedFields(TriggerContext mockedContext) {
        Map<String, Object> event = Map.of(
            "event", "conversation_started", "timestamp", 5L, "message_token", 9L, "type", "open",
            "context", "ctx", "subscribed", false, "user", Map.of("id", "u2", "name", "Ann"));

        Map<String, Object> result = ViberEventUtils.toOutput(event, ALL_EVENTS, mockedContext)
            .getFirst();

        assertEquals("open", result.get("type"));
        assertEquals("ctx", result.get("context"));
        assertEquals(false, result.get("subscribed"));
        assertEquals(Map.of("id", "u2", "name", "Ann"), result.get("user"));
    }

    @Test
    void testToOutputDownloadsPicture(
        TriggerContext mockedContext, Http mockedHttp, Executor mockedExecutor, Response mockedResponse,
        ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        FileEntry fileEntry = mock(FileEntry.class);

        when(mockedHttp.get("https://media.example.com/p.jpg"))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(fileEntry);

        Map<String, Object> event = Map.of(
            "event", "message", "sender", Map.of("id", "s1"),
            "message", Map.of("type", "picture", "media", "https://media.example.com/p.jpg"));

        Map<String, Object> result = ViberEventUtils.toOutput(event, ALL_EVENTS, mockedContext)
            .getFirst();

        Http.Configuration configuration = configurationBuilderArgumentCaptor.getValue()
            .build();

        assertSame(fileEntry, result.get("file"));
        assertEquals(ResponseType.BINARY, configuration.getResponseType());
        assertTrue(configuration.isDisableAuthorization());
    }

    @Test
    void testToOutputKeepsEventWhenMediaDownloadFails(TriggerContext mockedContext, Http mockedHttp) {
        when(mockedHttp.get("https://media.example.com/v.mp4"))
            .thenThrow(new IllegalStateException("boom"));

        Map<String, Object> event = Map.of(
            "event", "message", "sender", Map.of("id", "s1"),
            "message", Map.of("type", "video", "media", "https://media.example.com/v.mp4"));

        Map<String, Object> result = ViberEventUtils.toOutput(event, ALL_EVENTS, mockedContext)
            .getFirst();

        assertFalse(result.containsKey("file"));
        assertEquals(
            "https://media.example.com/v.mp4", ((Map<?, ?>) result.get("message")).get("media"));
    }

    @Test
    void testToOutputMapsStickerLocationAndContact(TriggerContext mockedContext) {
        Map<String, Object> sticker = Map.of("type", "sticker", "sticker_id", 40100);
        Map<String, Object> location = Map.of("type", "location", "location", Map.of("lat", 37.7, "lon", 2.1));
        Map<String, Object> contact = Map.of(
            "type", "contact", "contact", Map.of("name", "Al", "phone_number", "+1555"));

        for (Map<String, Object> message : List.of(sticker, location, contact)) {
            Map<String, Object> result = ViberEventUtils.toOutput(
                Map.of("event", "message", "sender", Map.of("id", "s1"), "message", message), ALL_EVENTS,
                mockedContext)
                .getFirst();

            assertEquals(message, result.get("message"));
            assertNull(result.get("file"));
        }
    }
}
