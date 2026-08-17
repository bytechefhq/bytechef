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

package com.bytechef.platform.workflow.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.commons.util.JsonUtils;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for the chat-only approval channel origin warning: a task that can only deliver its approval requests to the
 * chat channel, in a workflow that does not start from a chat trigger, pauses without a live approval card on
 * webhook/schedule runs.
 *
 * @author Ivica Cardic
 */
class WorkflowValidatorChatApprovalChannelTest {

    private static final String CHAT_ONLY_WARNING_FRAGMENT = "delivers approval requests only to the chat channel";

    @BeforeAll
    public static void beforeAll() {
        JsonUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @Test
    void validateWorkflowChatOnlyApprovalChannelWithoutChatTriggerWarns() {
        String workflow = """
            {
                "label": "Test Workflow",
                "triggers": [
                    {
                        "label": "Webhook",
                        "name": "trigger_1",
                        "type": "webhook/v1/webhook",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "Approval",
                        "name": "approval_1",
                        "type": "approval/v1/requestApproval",
                        "parameters": {},
                        "clusterElements": {
                            "approvalChannels": [
                                {
                                    "label": "Chat",
                                    "name": "chat_1",
                                    "type": "chat/v1/chat",
                                    "parameters": {}
                                }
                            ]
                        }
                    }
                ]
            }
            """;

        String warnings = validate(workflow);

        assertTrue(warnings.contains(CHAT_ONLY_WARNING_FRAGMENT), warnings);
        assertTrue(warnings.contains("approval_1"), warnings);
    }

    @Test
    void validateWorkflowChatChannelWithFallbackChannelDoesNotWarn() {
        String workflow = """
            {
                "label": "Test Workflow",
                "triggers": [
                    {
                        "label": "Webhook",
                        "name": "trigger_1",
                        "type": "webhook/v1/webhook",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "Approval",
                        "name": "approval_1",
                        "type": "approval/v1/requestApproval",
                        "parameters": {},
                        "clusterElements": {
                            "approvalChannels": [
                                {
                                    "label": "Chat",
                                    "name": "chat_1",
                                    "type": "chat/v1/chat",
                                    "parameters": {}
                                },
                                {
                                    "label": "Approval Task",
                                    "name": "approvalTask_1",
                                    "type": "approvalTask/v1/approvalTask",
                                    "parameters": {}
                                }
                            ]
                        }
                    }
                ]
            }
            """;

        String warnings = validate(workflow);

        assertFalse(warnings.contains(CHAT_ONLY_WARNING_FRAGMENT), warnings);
    }

    @Test
    void validateWorkflowChatOnlyApprovalChannelWithChatTriggerDoesNotWarn() {
        String workflow = """
            {
                "label": "Test Workflow",
                "triggers": [
                    {
                        "label": "Chat",
                        "name": "trigger_1",
                        "type": "chat/v1/newChatRequest",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "Approval",
                        "name": "approval_1",
                        "type": "approval/v1/requestApproval",
                        "parameters": {},
                        "clusterElements": {
                            "approvalChannels": [
                                {
                                    "label": "Chat",
                                    "name": "chat_1",
                                    "type": "chat/v1/chat",
                                    "parameters": {}
                                }
                            ]
                        }
                    }
                ]
            }
            """;

        String warnings = validate(workflow);

        assertFalse(warnings.contains(CHAT_ONLY_WARNING_FRAGMENT), warnings);
    }

    @Test
    void validateWorkflowGatedToolWithoutChannelsWithoutChatTriggerWarns() {
        String workflow = """
            {
                "label": "Test Workflow",
                "triggers": [
                    {
                        "label": "Webhook",
                        "name": "trigger_1",
                        "type": "webhook/v1/webhook",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "AI Agent",
                        "name": "aiAgent_1",
                        "type": "aiAgent/v1/chat",
                        "parameters": {},
                        "clusterElements": {
                            "tools": [
                                {
                                    "label": "Destructive",
                                    "name": "approvalGateTool_1",
                                    "type": "aiAgentUtils/v1/approvalGateTool",
                                    "parameters": {
                                        "name": "Destructive"
                                    },
                                    "clusterElements": {
                                        "tools": [
                                            {
                                                "label": "Send Message",
                                                "name": "slack_1",
                                                "type": "slack/v1/sendMessage",
                                                "parameters": {}
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
            """;

        String warnings = validate(workflow);

        assertTrue(warnings.contains(CHAT_ONLY_WARNING_FRAGMENT), warnings);
        assertTrue(warnings.contains("aiAgent_1"), warnings);
        assertTrue(warnings.contains("the default when no approval channels are configured"), warnings);
    }

    @Test
    void validateWorkflowGatedToolWithoutChannelsWithChatTriggerDoesNotWarn() {
        String workflow = """
            {
                "label": "Test Workflow",
                "triggers": [
                    {
                        "label": "Chat",
                        "name": "trigger_1",
                        "type": "chat/v1/newChatRequest",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "AI Agent",
                        "name": "aiAgent_1",
                        "type": "aiAgent/v1/chat",
                        "parameters": {},
                        "clusterElements": {
                            "tools": [
                                {
                                    "label": "Destructive",
                                    "name": "approvalGateTool_1",
                                    "type": "aiAgentUtils/v1/approvalGateTool",
                                    "parameters": {
                                        "name": "Destructive"
                                    },
                                    "clusterElements": {
                                        "tools": [
                                            {
                                                "label": "Send Message",
                                                "name": "slack_1",
                                                "type": "slack/v1/sendMessage",
                                                "parameters": {}
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
            """;

        String warnings = validate(workflow);

        assertFalse(warnings.contains(CHAT_ONLY_WARNING_FRAGMENT), warnings);
    }

    @Test
    void validateWorkflowUngatedToolWithoutChannelsDoesNotWarn() {
        String workflow = """
            {
                "label": "Test Workflow",
                "triggers": [
                    {
                        "label": "Webhook",
                        "name": "trigger_1",
                        "type": "webhook/v1/webhook",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "AI Agent",
                        "name": "aiAgent_1",
                        "type": "aiAgent/v1/chat",
                        "parameters": {},
                        "clusterElements": {
                            "tools": [
                                {
                                    "label": "Send Message",
                                    "name": "slack_1",
                                    "type": "slack/v1/sendMessage",
                                    "parameters": {}
                                }
                            ]
                        }
                    }
                ]
            }
            """;

        String warnings = validate(workflow);

        assertFalse(warnings.contains(CHAT_ONLY_WARNING_FRAGMENT), warnings);
    }

    @Test
    void validateWorkflowGatedToolWithFallbackChannelDoesNotWarn() {
        String workflow = """
            {
                "label": "Test Workflow",
                "triggers": [
                    {
                        "label": "Webhook",
                        "name": "trigger_1",
                        "type": "webhook/v1/webhook",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "AI Agent",
                        "name": "aiAgent_1",
                        "type": "aiAgent/v1/chat",
                        "parameters": {},
                        "clusterElements": {
                            "tools": [
                                {
                                    "label": "Destructive",
                                    "name": "approvalGateTool_1",
                                    "type": "aiAgentUtils/v1/approvalGateTool",
                                    "parameters": {
                                        "name": "Destructive"
                                    },
                                    "clusterElements": {
                                        "approvalChannels": [
                                            {
                                                "label": "Slack",
                                                "name": "slackChannel_1",
                                                "type": "slack/v1/approvalChannel",
                                                "parameters": {}
                                            }
                                        ],
                                        "tools": [
                                            {
                                                "label": "Send Message",
                                                "name": "slack_1",
                                                "type": "slack/v1/sendMessage",
                                                "parameters": {}
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
            """;

        String warnings = validate(workflow);

        assertFalse(warnings.contains(CHAT_ONLY_WARNING_FRAGMENT), warnings);
    }

    private static String validate(String workflow) {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateWorkflow(
            workflow, (taskType, kind) -> null, (taskType, kind, warningsBuilder) -> null, taskType -> null,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        return warnings.toString();
    }
}
