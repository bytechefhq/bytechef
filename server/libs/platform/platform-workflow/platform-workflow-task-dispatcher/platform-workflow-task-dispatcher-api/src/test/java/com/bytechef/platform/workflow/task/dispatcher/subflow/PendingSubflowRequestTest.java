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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class PendingSubflowRequestTest {

    @Test
    void testJsonRoundTrip() {
        PendingSubflowRequest pendingSubflowRequest = new PendingSubflowRequest(
            "wf-123", "newWorkflowCall", Map.of("amount", 42));

        PendingSubflowRequest roundTripped = JsonUtils.read(
            JsonUtils.write(pendingSubflowRequest), PendingSubflowRequest.class);

        assertThat(roundTripped).isEqualTo(pendingSubflowRequest);
    }

    @Test
    void testConvertValueFromMap() {
        Map<String, Object> map = Map.of(
            "workflowId", "wf-123", "inputsName", "newWorkflowCall", "inputs", Map.of("amount", 42));

        PendingSubflowRequest pendingSubflowRequest = ConvertUtils.convertValue(map, PendingSubflowRequest.class);

        assertThat(pendingSubflowRequest.workflowId()).isEqualTo("wf-123");
        assertThat(pendingSubflowRequest.inputsName()).isEqualTo("newWorkflowCall");
        assertThat(pendingSubflowRequest.inputs()).isEqualTo(Map.of("amount", 42));
    }

    @Test
    void testRejectsBlankIdentifiers() {
        assertThatThrownBy(() -> new PendingSubflowRequest(" ", "newWorkflowCall", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PendingSubflowRequest("wf-123", "", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCopiesInputsSoLaterChangesDoNotLeakIn() {
        Map<String, Object> inputs = new HashMap<>();

        inputs.put("amount", 42);

        PendingSubflowRequest pendingSubflowRequest = new PendingSubflowRequest("wf-123", "newWorkflowCall", inputs);

        inputs.put("amount", 43);

        assertThat(pendingSubflowRequest.inputs()).isEqualTo(Map.of("amount", 42));
        assertThatThrownBy(() -> ((Map<String, Object>) pendingSubflowRequest.inputs()).put("other", 1))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
