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

package org.springframework.ai.session.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.store.StoredSession.StoredEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_INFERRED")
class StoredSessionTest {

    private static final int MAX_ARCHIVED_EVENTS = StoredSession.DEFAULT_MAX_ARCHIVED_EVENTS;

    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();

    @Test
    void testEveryWriteIncrementsTheRevision() {
        StoredSession storedSession = StoredSession.fromSession(newSession(), 0L, List.of());

        StoredSession resavedSession = storedSession.withSession(newSession());
        StoredSession appendedSession = resavedSession.appendEvent(newStoredEvent("event-1"), MAX_ARCHIVED_EVENTS);
        StoredSession replacedSession = appendedSession.replaceEvents(List.of(), MAX_ARCHIVED_EVENTS);

        assertEquals(0L, storedSession.revision());
        assertEquals(1L, resavedSession.revision());
        assertEquals(0L, resavedSession.version());
        assertEquals(2L, appendedSession.revision());
        assertEquals(1L, appendedSession.version());
        assertEquals(3L, replacedSession.revision());
        assertEquals(2L, replacedSession.version());
    }

    @Test
    void testAppendEventAddsTheEventAfterExistingEvents() {
        StoredSession storedSession = StoredSession.fromSession(newSession(), 4L, List.of(newStoredEvent("event-1")));

        StoredSession appendedSession = storedSession.appendEvent(newStoredEvent("event-2"), MAX_ARCHIVED_EVENTS);

        assertEquals(List.of("event-1", "event-2"), eventIds(appendedSession));
        assertEquals(5L, appendedSession.version());
        assertEquals(List.of("event-1"), eventIds(storedSession));
    }

    @Test
    void testReplaceEventsSwapsTheEvents() {
        StoredSession storedSession = StoredSession.fromSession(newSession(), 4L, List.of(newStoredEvent("event-1")));

        StoredSession replacedSession =
            storedSession.replaceEvents(List.of(newStoredEvent("event-2")), MAX_ARCHIVED_EVENTS);

        assertEquals(List.of("event-2"), eventIds(replacedSession));
        assertEquals(5L, replacedSession.version());
        assertEquals(1L, replacedSession.revision());
    }

    @Test
    void testAppendEventRejectsEventOfAnotherSession() {
        StoredSession storedSession = StoredSession.fromSession(newSession(), 0L, List.of());

        StoredEvent foreignEvent = new StoredEvent(
            "event-1", "session-2", 1L, MessageType.USER, "hello", null, false, false, Map.of());

        assertThrows(IllegalArgumentException.class,
            () -> storedSession.appendEvent(foreignEvent, MAX_ARCHIVED_EVENTS));
    }

    @Test
    void testReplaceEventsRejectsEventOfAnotherSession() {
        StoredSession storedSession = StoredSession.fromSession(newSession(), 0L, List.of());

        StoredEvent foreignEvent = new StoredEvent(
            "event-1", "session-2", 1L, MessageType.USER, "hello", null, false, false, Map.of());

        assertThrows(
            IllegalArgumentException.class,
            () -> storedSession.replaceEvents(List.of(foreignEvent), MAX_ARCHIVED_EVENTS));
    }

    @Test
    void testConstructorRejectsEventOfAnotherSession() {
        StoredEvent foreignEvent = new StoredEvent(
            "event-1", "session-2", 1L, MessageType.USER, "hello", null, false, false, Map.of());

        assertThrows(
            IllegalArgumentException.class,
            () -> new StoredSession("session-1", "user-1", 1L, null, Map.of(), 0L, 0L, List.of(foreignEvent)));
    }

    @Test
    void testReadRejectsDocumentHoldingEventOfAnotherSession() {
        String json = """
            {"id":"session-1","userId":"user-1","createdAtEpochMilli":1,"expiresAtEpochMilli":null,
             "metadata":{},"version":1,"revision":1,
             "events":[{"id":"event-1","sessionId":"session-2","timestampEpochMilli":2,"messageType":"USER",
                        "messageContent":"hi","messageData":null,"synthetic":false,"archived":false,"metadata":{}}]}
            """;

        assertThrows(JacksonException.class, () -> jsonMapper.readValue(json, StoredSession.class));
    }

    @Test
    void testReadsToolEventWithoutMessageContent() {
        String json = """
            {"id":"session-1","userId":"user-1","createdAtEpochMilli":1,"expiresAtEpochMilli":null,
             "metadata":{},"version":1,"revision":1,
             "events":[{"id":"event-1","sessionId":"session-1","timestampEpochMilli":2,"messageType":"TOOL",
                        "messageContent":null,"synthetic":false,"archived":false,"metadata":{},
                        "messageData":"[{\\"id\\":\\"call-1\\",\\"name\\":\\"tool\\",\\"responseData\\":\\"{}\\"}]"}]}
            """;

        StoredSession storedSession = jsonMapper.readValue(json, StoredSession.class);

        StoredEvent storedEvent = storedSession.events()
            .getFirst();

        assertNull(storedEvent.messageContent());
        assertInstanceOf(ToolResponseMessage.class, storedEvent.toEvent(jsonMapper)
            .getMessage());
    }

    @Test
    void testWithSessionRejectsSessionWithDifferentId() {
        StoredSession storedSession = StoredSession.fromSession(newSession(), 0L, List.of());

        Session otherSession = Session.builder()
            .id("session-2")
            .userId("user-1")
            .createdAt(Instant.now())
            .build();

        assertThrows(IllegalArgumentException.class, () -> storedSession.withSession(otherSession));
    }

    @Test
    void testReadsEventStoredWithMessageTypeName() {
        String json = """
            {"id":"session-1","userId":"user-1","createdAtEpochMilli":1,"expiresAtEpochMilli":null,
             "metadata":{},"version":1,"revision":1,
             "events":[{"id":"event-1","sessionId":"session-1","timestampEpochMilli":2,"messageType":"ASSISTANT",
                        "messageContent":"hi","messageData":null,"synthetic":false,"archived":true,"metadata":{}}]}
            """;

        StoredSession storedSession = jsonMapper.readValue(json, StoredSession.class);

        StoredEvent storedEvent = storedSession.events()
            .getFirst();

        assertEquals(MessageType.ASSISTANT, storedEvent.messageType());

        SessionEvent sessionEvent = storedEvent.toEvent(jsonMapper);

        assertInstanceOf(AssistantMessage.class, sessionEvent.getMessage());
        assertEquals("hi", sessionEvent.getMessage()
            .getText());
        assertTrue(sessionEvent.isArchived());
        assertTrue(jsonMapper.writeValueAsString(storedSession)
            .contains("\"messageType\":\"ASSISTANT\""));
    }

    @Test
    void testAppendEventPrunesTheOldestArchivedEventsAndBumpsTheRevision() {
        List<StoredEvent> events = List.of(
            newStoredEvent("archived-1", true), newStoredEvent("active-1", false), newStoredEvent("archived-2", true),
            newStoredEvent("archived-3", true), newStoredEvent("active-2", false));

        StoredSession storedSession = StoredSession.fromSession(newSession(), 7L, events);

        StoredSession appendedSession = storedSession.appendEvent(newStoredEvent("archived-4", true), 2);

        assertEquals(List.of("active-1", "archived-3", "active-2", "archived-4"), eventIds(appendedSession));
        assertEquals(8L, appendedSession.version());
        assertEquals(storedSession.revision() + 1, appendedSession.revision());
    }

    @Test
    void testReplaceEventsPrunesTheOldestArchivedEventsAndBumpsTheRevision() {
        StoredSession storedSession = StoredSession.fromSession(newSession(), 0L, List.of());

        StoredSession replacedSession = storedSession.replaceEvents(
            List.of(newStoredEvent("archived-1", true), newStoredEvent("active-1", false)), 0);

        assertEquals(List.of("active-1"), eventIds(replacedSession));
        assertEquals(1L, replacedSession.version());
        assertEquals(storedSession.revision() + 1, replacedSession.revision());
    }

    @Test
    void testAppendEventWithZeroArchivedEventLimitKeepsOnlyActiveEvents() {
        StoredSession storedSession = StoredSession.fromSession(
            newSession(), 0L, List.of(newStoredEvent("archived-1", true), newStoredEvent("active-1", false)));

        StoredSession archivedAppendedSession = storedSession.appendEvent(newStoredEvent("archived-2", true), 0);

        assertEquals(List.of("active-1"), eventIds(archivedAppendedSession));
        assertEquals(1L, archivedAppendedSession.version());

        StoredSession activeAppendedSession = archivedAppendedSession.appendEvent(
            newStoredEvent("active-2", false), 0);

        assertEquals(List.of("active-1", "active-2"), eventIds(activeAppendedSession));
        assertEquals(2L, activeAppendedSession.version());
    }

    @Test
    void testWritesKeepEverythingUnderTheArchivedEventLimit() {
        StoredSession storedSession = StoredSession.fromSession(newSession(), 0L, List.of());

        StoredSession replacedSession = storedSession.replaceEvents(
            List.of(newStoredEvent("archived-1", true), newStoredEvent("active-1", false)), 1);

        assertEquals(List.of("archived-1", "active-1"), eventIds(replacedSession));
        assertThrows(
            IllegalArgumentException.class, () -> storedSession.replaceEvents(List.of(), -1));
        assertThrows(
            IllegalArgumentException.class, () -> storedSession.appendEvent(newStoredEvent("event-1"), -1));
    }

    @Test
    void testStoredEventRejectsMissingRequiredFields() {
        assertThrows(
            NullPointerException.class,
            () -> new StoredEvent(null, "session-1", 1L, MessageType.USER, "hello", null, false, false, Map.of()));
        assertThrows(
            NullPointerException.class,
            () -> new StoredEvent("event-1", null, 1L, MessageType.USER, "hello", null, false, false, Map.of()));
        assertThrows(
            NullPointerException.class,
            () -> new StoredEvent("event-1", "session-1", 1L, null, "hello", null, false, false, Map.of()));
    }

    @Test
    void testStoredEventRequiresContentForUserAndSystemMessages() {
        assertThrows(
            NullPointerException.class,
            () -> new StoredEvent("event-1", "session-1", 1L, MessageType.USER, null, null, false, false, Map.of()));
        assertThrows(
            NullPointerException.class,
            () -> new StoredEvent("event-1", "session-1", 1L, MessageType.SYSTEM, null, null, false, false, Map.of()));
    }

    @Test
    void testStoredEventAllowsMissingContentForAssistantAndToolMessages() {
        StoredEvent assistantEvent = new StoredEvent(
            "event-1", "session-1", 1L, MessageType.ASSISTANT, null, null, false, false, Map.of());
        StoredEvent toolEvent = new StoredEvent(
            "event-2", "session-1", 1L, MessageType.TOOL, null, null, false, false, Map.of());

        assertNull(assistantEvent.messageContent());
        assertNull(toolEvent.messageContent());
    }

    @Test
    void testDocumentWithoutRevisionReadsAsRevisionZero() {
        String json = """
            {"id":"session-1","userId":"user-1","createdAtEpochMilli":1,"expiresAtEpochMilli":null,
             "metadata":{},"version":3,"events":[]}
            """;

        StoredSession storedSession = jsonMapper.readValue(json, StoredSession.class);

        assertEquals(3L, storedSession.version());
        assertEquals(0L, storedSession.revision());
    }

    @Test
    void testRejectsInvalidDocuments() {
        assertThrows(
            NullPointerException.class,
            () -> new StoredSession(null, "user-1", 1L, null, Map.of(), 0L, 0L, List.of()));
        assertThrows(
            IllegalArgumentException.class,
            () -> new StoredSession("session-1", "user-1", 1L, null, Map.of(), -1L, 0L, List.of()));
        assertThrows(
            IllegalArgumentException.class,
            () -> new StoredSession("session-1", "user-1", 1L, null, Map.of(), 0L, -1L, List.of()));
    }

    private static StoredEvent newStoredEvent(String eventId) {
        return newStoredEvent(eventId, false);
    }

    private static StoredEvent newStoredEvent(String eventId, boolean archived) {
        return new StoredEvent(eventId, "session-1", 1L, MessageType.USER, "hello", null, false, archived, Map.of());
    }

    private static List<String> eventIds(StoredSession storedSession) {
        return storedSession.events()
            .stream()
            .map(StoredEvent::id)
            .toList();
    }

    private static Session newSession() {
        return Session.builder()
            .id("session-1")
            .userId("user-1")
            .createdAt(Instant.now())
            .build();
    }
}
