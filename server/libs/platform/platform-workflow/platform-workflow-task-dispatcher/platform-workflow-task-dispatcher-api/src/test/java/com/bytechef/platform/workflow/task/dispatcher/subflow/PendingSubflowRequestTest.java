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

package com.bytechef.platform.workflow.task.dispatcher.subflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.bytechef.platform.constant.PlatformType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PendingSubflowRequestTest {

    @Test
    void testJacksonRoundTrip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        PendingSubflowRequest request = new PendingSubflowRequest(
            "wf-123", "newWorkflowCall", Map.of("amount", 42), false, PlatformType.AUTOMATION);

        String json = objectMapper.writeValueAsString(request);
        PendingSubflowRequest result = objectMapper.readValue(json, PendingSubflowRequest.class);

        assertEquals(request, result);
    }

    @Test
    void testConvertValueFromMap() {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> map = Map.of(
            "workflowId", "wf-123", "inputsName", "newWorkflowCall", "inputs", Map.of("amount", 42),
            "editorEnvironment", false, "platformType", "AUTOMATION");

        PendingSubflowRequest result = objectMapper.convertValue(map, PendingSubflowRequest.class);

        assertEquals("wf-123", result.workflowId());
        assertEquals("newWorkflowCall", result.inputsName());
        assertEquals(PlatformType.AUTOMATION, result.platformType());
        assertFalse(result.editorEnvironment());
        assertEquals(Map.of("amount", 42), result.inputs());
    }
}
