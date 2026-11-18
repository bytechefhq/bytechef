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

package com.bytechef.platform.ai.tool.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ToolNameUtilsTest {

    @Test
    void testGetDefaultToolNameJoinsTheUpperCasedComponentAndElementNames() {
        assertEquals("ACME_GET_ORDER", ToolNameUtils.getDefaultToolName("acme", "getOrder"));
    }

    @Test
    void testGetDefaultToolNameKeepsASingleWordElementName() {
        assertEquals("SLACK_POST", ToolNameUtils.getDefaultToolName("slack", "post"));
    }

    @Test
    void testGetWorkflowToolNameFallsBackToTheWorkflowLabel() {
        assertEquals("SEND_EMAIL", ToolNameUtils.getWorkflowToolName(Map.of(), "Send Email"));
    }

    @Test
    void testGetWorkflowToolNamePrefersTheConfiguredName() {
        assertEquals(
            "sendEmail", ToolNameUtils.getWorkflowToolName(Map.of("toolName", "sendEmail"), "Send Email"));
    }

    @Test
    void testGetWorkflowToolNameKeepsTheCaseOfACleanedConfiguredName() {
        assertEquals(
            "send_Email", ToolNameUtils.getWorkflowToolName(Map.of("toolName", "send.Email"), "Send Email"));
    }

    @Test
    void testGetWorkflowToolNameCutsAConfiguredNameToTheMaximumLength() {
        String toolName = ToolNameUtils.getWorkflowToolName(Map.of("toolName", "a".repeat(70)), "Send Email");

        assertEquals("a".repeat(64), toolName);
    }

    @Test
    void testGetWorkflowToolNameReturnsNullWithoutAConfiguredNameOrLabel() {
        assertNull(ToolNameUtils.getWorkflowToolName(Map.of(), null));
    }

    @Test
    void testGetWorkflowToolNameReplacesCharactersInTheLabel() {
        assertEquals("ORDER_SYNC_V2", ToolNameUtils.getWorkflowToolName(Map.of(), "Order Sync (v2)"));
    }

    @Test
    void testGetWorkflowToolNameUpperCasesAnAlreadyValidLabel() {
        assertEquals("ORDER-SYNC_2", ToolNameUtils.getWorkflowToolName(Map.of(), "order-sync_2"));
    }

    @Test
    void testGetWorkflowToolNameReturnsNullForALabelWithNothingUsable() {
        assertNull(ToolNameUtils.getWorkflowToolName(Map.of(), "()"));
    }
}
