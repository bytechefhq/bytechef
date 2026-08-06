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

package com.bytechef.workflow.guest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.workflow.definition.TaskContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class GuestTaskContextTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void testComponentSendsRequestJsonAndParsesResultJson() throws Exception {
        RecordingHostBridge hostBridge = new RecordingHostBridge("{\"y\": 2}");

        GuestTaskContext guestTaskContext = new GuestTaskContext(hostBridge);

        Object result = guestTaskContext.component("mock", "doIt", Map.of("x", 1), "conn");

        Map<String, ?> request = readMap(hostBridge.requestJson);

        assertEquals("mock", request.get("componentName"));
        assertEquals("doIt", request.get("actionName"));
        assertEquals(Map.of("x", 1), request.get("input"));
        assertEquals("conn", request.get("connectionName"));
        assertEquals(Map.of("y", 2), result);
    }

    @Test
    void testComponentSendsClusterElementsAndOmitsThemWhenThereAreNone() throws Exception {
        RecordingHostBridge hostBridge = new RecordingHostBridge("null");

        GuestTaskContext guestTaskContext = new GuestTaskContext(hostBridge);

        Map<String, ?> clusterElements = Map.of("model", Map.of("type", "openAi/v1/model"));

        guestTaskContext.component("aiAgent", "chat", Map.of(), null, clusterElements);

        assertEquals(clusterElements, readMap(hostBridge.requestJson).get("clusterElements"));

        // The four-argument call is every existing task's call, so it must not start sending an empty map.
        guestTaskContext.component("mock", "doIt", Map.of(), "conn");

        assertFalse(readMap(hostBridge.requestJson).containsKey("clusterElements"));
    }

    @Test
    void testComponentOmitsNullConnectionNameAndDefaultsNullInput() throws Exception {
        RecordingHostBridge hostBridge = new RecordingHostBridge("null");

        GuestTaskContext guestTaskContext = new GuestTaskContext(hostBridge);

        Object result = guestTaskContext.component("mock", "doIt", null, null);

        Map<String, ?> request = readMap(hostBridge.requestJson);

        assertEquals(Map.of(), request.get("input"));
        assertFalse(request.containsKey("connectionName"));
        assertNull(result);
    }

    @Test
    void testConnectionParametersParsesHostJson() throws Exception {
        RecordingHostBridge hostBridge = new RecordingHostBridge(null);

        hostBridge.connectionParametersJson = "{\"region\": \"eu\"}";

        GuestTaskContext guestTaskContext = new GuestTaskContext(hostBridge);

        assertEquals(Map.of("region", "eu"), guestTaskContext.connection("slack-prod"));
        assertEquals("slack-prod", hostBridge.connectionName);
    }

    @Test
    void testLogDelegatesToHostBridge() {
        RecordingHostBridge hostBridge = new RecordingHostBridge(null);

        GuestTaskContext guestTaskContext = new GuestTaskContext(hostBridge);

        guestTaskContext.log(TaskContext.LogLevel.WARN, "log message");

        assertEquals("WARN", hostBridge.logLevel);
        assertEquals("log message", hostBridge.logMessage);
    }

    @Test
    void testInputReadsTheHostSnapshotAndFailsOnAnUnknownName() {
        RecordingHostBridge hostBridge = new RecordingHostBridge(null);

        hostBridge.inputJson = "{\"input\":{\"email\":\"a@b.com\"},\"my-task1\":{\"id\":3}}";

        GuestTaskContext guestTaskContext = new GuestTaskContext(hostBridge);

        assertEquals(Map.of("email", "a@b.com"), guestTaskContext.input()
            .get("input"));
        // input(name) resolves on the host, so the host's explanation of a missing name reaches the guest.
        hostBridge.inputNamedJson = "{\"id\":3}";

        assertEquals(Map.of("id", 3), guestTaskContext.input("my-task1"));
        assertEquals("my-task1", hostBridge.inputNamedName);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> readMap(String json) throws Exception {
        return OBJECT_MAPPER.readValue(json, Map.class);
    }

    private static final class RecordingHostBridge implements HostBridge {

        private final String resultJson;

        private String requestJson;
        private String connectionName;
        private String connectionParametersJson;
        private String inputJson;
        private String inputNamedJson;
        private String inputNamedName;
        private String logLevel;
        private String logMessage;

        private RecordingHostBridge(String resultJson) {
            this.resultJson = resultJson;
        }

        @Override
        public String componentExecute(String requestJson) {
            this.requestJson = requestJson;

            return resultJson;
        }

        @Override
        public String input() {
            return inputJson;
        }

        @Override
        public String input(String name) {
            this.inputNamedName = name;

            return inputNamedJson;
        }

        @Override
        public String parameters() {
            return null;
        }

        @Override
        public String connection(String connectionName) {
            this.connectionName = connectionName;

            return connectionParametersJson;
        }

        @Override
        public void log(String level, String message) {
            this.logLevel = level;
            this.logMessage = message;
        }
    }
}
