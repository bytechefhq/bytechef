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

package com.bytechef.workflow.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TaskContextTest {

    @Test
    void testComponentWithoutConnectionNamePassesNoConnectionName() throws Exception {
        List<Object> calledArguments = new ArrayList<>();

        TaskContext taskContext = new RecordingTaskContext(calledArguments);

        Object result = taskContext.component("googleMail", "searchEmail", Map.of("maxResults", 10));

        assertEquals("result", result);
        assertEquals("googleMail", calledArguments.get(0));
        assertEquals("searchEmail", calledArguments.get(1));
        assertEquals(Map.of("maxResults", 10), calledArguments.get(2));
        assertNull(calledArguments.get(3));
        assertNull(calledArguments.get(4));
    }

    private record RecordingTaskContext(List<Object> calledArguments) implements TaskContext {

        @Override
        public Object component(
            String componentName, String actionName, Map<String, ?> input, String connectionName,
            Map<String, ?> clusterElements) {

            calledArguments.add(componentName);
            calledArguments.add(actionName);
            calledArguments.add(input);
            calledArguments.add(connectionName);
            calledArguments.add(clusterElements);

            return "result";
        }

        @Override
        public Map<String, ?> input() {
            return Map.of();
        }

        @Override
        public Object input(String name) {
            return null;
        }

        @Override
        public Map<String, ?> parameters() {
            return Map.of();
        }

        @Override
        public Map<String, ?> connection(String connectionName) {
            return Map.of();
        }

        @Override
        public void log(LogLevel level, String message) {
            calledArguments.add(message);
        }
    }
}
