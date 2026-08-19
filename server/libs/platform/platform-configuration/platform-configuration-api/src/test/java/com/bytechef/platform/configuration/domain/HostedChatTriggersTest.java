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

package com.bytechef.platform.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class HostedChatTriggersTest {

    @Test
    void testNoTriggersIsNotHostedChat() {
        assertFalse(HostedChatTriggers.hasHostedChatTrigger(List.of()));
    }

    @Test
    void testNonChatTriggerIsNotHostedChat() {
        assertFalse(
            HostedChatTriggers
                .hasHostedChatTrigger(List.of(workflowTrigger("webhook", "webhook/v1/newWebhook", null))));
    }

    @Test
    void testChatTriggerWithoutModeIsHostedChat() {
        assertTrue(
            HostedChatTriggers.hasHostedChatTrigger(List.of(workflowTrigger("chat", "chat/v1/newChatRequest", null))));
    }

    @Test
    void testChatTriggerWithModeOneIsHostedChat() {
        assertTrue(
            HostedChatTriggers.hasHostedChatTrigger(List.of(workflowTrigger("chat", "chat/v1/newChatRequest", 1))));
    }

    @Test
    void testChatTriggerWithModeTwoIsNotHostedChat() {
        assertFalse(
            HostedChatTriggers.hasHostedChatTrigger(List.of(workflowTrigger("chat", "chat/v1/newChatRequest", 2))));
    }

    @Test
    void testChatTriggerWithNonNumberModeIsNotHostedChat() {
        assertFalse(
            HostedChatTriggers.hasHostedChatTrigger(List.of(workflowTrigger("chat", "chat/v1/newChatRequest", "1"))));
    }

    /**
     * Pins the asymmetry between the two conditions: the {@code chat/} prefix is looked for in every trigger, but the
     * mode is read only from the first one. A chat trigger sitting behind a non-hosted first trigger therefore does not
     * make the workflow a hosted chat.
     */
    @Test
    void testChatTriggerBehindFirstTriggerWithModeTwoIsNotHostedChat() {
        List<WorkflowTrigger> workflowTriggers = List.of(
            workflowTrigger("external", "chat/v1/newChatRequest", 2),
            workflowTrigger("hosted", "chat/v1/newChatRequest", 1));

        assertFalse(HostedChatTriggers.hasHostedChatTrigger(workflowTriggers));
    }

    @Test
    void testChatTriggerBehindFirstTriggerWithoutModeIsHostedChat() {
        List<WorkflowTrigger> workflowTriggers = List.of(
            workflowTrigger("webhook", "webhook/v1/newWebhook", null),
            workflowTrigger("chat", "chat/v1/newChatRequest", 1));

        assertTrue(HostedChatTriggers.hasHostedChatTrigger(workflowTriggers));
    }

    @Test
    void testIsChatTriggerType() {
        assertTrue(HostedChatTriggers.isChatTriggerType("chat/v1/newChatRequest"));
        assertFalse(HostedChatTriggers.isChatTriggerType("webhook/v1/newWebhook"));
        assertFalse(HostedChatTriggers.isChatTriggerType("chat"));
        assertFalse(HostedChatTriggers.isChatTriggerType(""));
        assertFalse(HostedChatTriggers.isChatTriggerType(null));
    }

    private static WorkflowTrigger workflowTrigger(String name, String type, @Nullable Object mode) {
        Map<String, Object> parameters = new HashMap<>();

        if (mode != null) {
            parameters.put("mode", mode);
        }

        return new WorkflowTrigger(Map.of("name", name, "type", type, "parameters", parameters));
    }
}
