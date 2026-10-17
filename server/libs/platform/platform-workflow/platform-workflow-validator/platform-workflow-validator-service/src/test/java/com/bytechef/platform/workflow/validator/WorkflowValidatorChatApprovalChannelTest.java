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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.NodeValidationIssue;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueSeverity;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
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

        List<NodeValidationIssue> nodeValidationIssues = NodeValidationIssueParser.parse(
            List.of(), List.of(warnings.strip()));

        assertEquals(1, nodeValidationIssues.size(), warnings);

        NodeValidationIssue nodeValidationIssue = nodeValidationIssues.getFirst();

        assertEquals("approval_1", nodeValidationIssue.nodeName());
        assertEquals(WorkflowIssueSeverity.WARNING, nodeValidationIssue.severity());
        assertTrue(nodeValidationIssue.message()
            .contains(CHAT_ONLY_WARNING_FRAGMENT));
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
                                    "name": "approvalGate_1",
                                    "type": "aiAgentUtils/v1/approvalGate",
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
        assertTrue(warnings.startsWith("[approvalGate_1] "), warnings);
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
                                    "name": "approvalGate_1",
                                    "type": "aiAgentUtils/v1/approvalGate",
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
                                    "name": "approvalGate_1",
                                    "type": "aiAgentUtils/v1/approvalGate",
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

    @Nested
    class ApprovalGatePlacement {

        @Test
        void validateWorkflowApprovalGateUnderSubagentFails() {
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
                                        "label": "Task Tool",
                                        "name": "taskTool_1",
                                        "type": "aiAgentUtils/v1/taskTool",
                                        "parameters": {},
                                        "clusterElements": {
                                            "subagent": [
                                                {
                                                    "label": "Notifier",
                                                    "name": "subagentTool_1",
                                                    "type": "aiAgentUtils/v1/subagentTool",
                                                    "parameters": {},
                                                    "clusterElements": {
                                                        "tools": [
                                                            {
                                                                "label": "Mail gate",
                                                                "name": "approvalGate_1",
                                                                "type": "aiAgentUtils/v1/approvalGate",
                                                                "parameters": {},
                                                                "clusterElements": {
                                                                    "approvalChannels": [
                                                                        {
                                                                            "label": "Approval Task",
                                                                            "name": "approvalTaskChannel_1",
                                                                            "type": "approvalTask/v1/approvalTask",
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
                                    }
                                ]
                            }
                        }
                    ]
                }
                """;

            String errors = validateErrors(workflow);

            assertTrue(
                errors.contains("[approvalGate_1] An approval gate cannot be placed under a subagent"), errors);
        }

        @Test
        void validateWorkflowApprovalGateOnAgentPasses() {
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
                                        "label": "Mail gate",
                                        "name": "approvalGate_1",
                                        "type": "aiAgentUtils/v1/approvalGate",
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
                        }
                    ]
                }
                """;

            String errors = validateErrors(workflow);

            assertFalse(errors.contains("approval gate"), errors);
            assertFalse(errors.contains("requestApproval"), errors);
        }

        @Test
        void validateWorkflowRequestApprovalAndNestedGateInsideGateFail() {
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
                                        "label": "Gate",
                                        "name": "approvalGate_1",
                                        "type": "aiAgentUtils/v1/approvalGate",
                                        "parameters": {},
                                        "clusterElements": {
                                            "tools": [
                                                {
                                                    "label": "Request Approval",
                                                    "name": "requestApproval_1",
                                                    "type": "approval/v1/requestApproval",
                                                    "parameters": {}
                                                },
                                                {
                                                    "label": "Inner gate",
                                                    "name": "approvalGate_2",
                                                    "type": "aiAgentUtils/v1/approvalGate",
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

            String errors = validateErrors(workflow);

            assertTrue(
                errors.contains("[requestApproval_1] 'requestApproval' cannot be attached to an approval gate"),
                errors);
            assertTrue(
                errors.contains("[approvalGate_2] An approval gate cannot be attached to another approval gate"),
                errors);
        }
    }

    @Nested
    class SubagentApprovalPlacement {

        @Test
        void validateWorkflowRequestApprovalOnASubagentFails() {
            String errors = validateErrors(
                agentWorkflow(
                    taskTool(
                        "taskTool_1",
                        subagent("subagentTool_1", tool("requestApproval_1", "approval/v1/requestApproval")))));

            assertTrue(
                errors
                    .contains("[requestApproval_1] 'requestApproval' cannot be attached to subagent 'subagentTool_1'"),
                errors);
        }

        @Test
        void validateWorkflowApprovalGateOnASubagentOfANestedTaskToolFails() {
            String errors = validateErrors(
                agentWorkflow(
                    taskTool(
                        "taskTool_1",
                        subagent(
                            "subagentTool_1",
                            taskTool(
                                "taskTool_2",
                                subagent(
                                    "subagentTool_2", tool("approvalGate_1", "aiAgentUtils/v1/approvalGate"),
                                    tool("requestApproval_1", "approval/v1/requestApproval")))))));

            assertTrue(
                errors.contains("[approvalGate_1] An approval gate cannot be placed under a subagent"), errors);
            assertTrue(
                errors
                    .contains("[requestApproval_1] 'requestApproval' cannot be attached to subagent 'subagentTool_2'"),
                errors);
        }

        @Test
        void validateWorkflowReportsANestedTaskToolPlacementOnce() {
            String errors = validateErrors(
                agentWorkflow(
                    taskTool(
                        "taskTool_1",
                        subagent(
                            "subagentTool_1",
                            taskTool(
                                "taskTool_2",
                                subagent("subagentTool_2", tool("approvalGate_1", "aiAgentUtils/v1/approvalGate")))))));

            assertEquals(
                errors.indexOf("[approvalGate_1] An approval gate cannot be placed under a subagent"),
                errors.lastIndexOf("[approvalGate_1] An approval gate cannot be placed under a subagent"), errors);
        }

        @Test
        void validateWorkflowApprovalToolsTheRuntimeRunsOnTheAgentPass() {
            String errors = validateErrors(
                agentWorkflow(
                    tool("requestApproval_1", "approval/v1/requestApproval"),
                    taskTool(
                        "taskTool_1",
                        subagent(
                            "subagentTool_1",
                            toolWithChildren(
                                "callAgent_1", "aiAgent/v1/chat", "tools",
                                tool("approvalGate_1", "aiAgentUtils/v1/approvalGate"))))));

            assertFalse(errors.contains("subagent"), errors);
        }

        private static String agentWorkflow(String... tools) {
            return """
                {
                    "label": "Test Workflow",
                    "triggers": [
                        {"label": "Webhook", "name": "trigger_1", "type": "webhook/v1/webhook", "parameters": {}}
                    ],
                    "tasks": [
                        {
                            "label": "AI Agent",
                            "name": "aiAgent_1",
                            "type": "aiAgent/v1/chat",
                            "parameters": {},
                            "clusterElements": {"tools": [TOOLS]}
                        }
                    ]
                }
                """.replace("[TOOLS]", "[" + String.join(",", tools) + "]");
        }

        private static String taskTool(String name, String... subagents) {
            return toolWithChildren(name, "aiAgentUtils/v1/taskTool", "subagent", subagents);
        }

        private static String subagent(String name, String... tools) {
            return toolWithChildren(name, "aiAgentUtils/v1/subagentTool", "tools", tools);
        }

        private static String tool(String name, String type) {
            return "{\"label\": \"" + name + "\", \"name\": \"" + name + "\", \"type\": \"" + type +
                "\", \"parameters\": {}}";
        }

        private static String toolWithChildren(String name, String type, String key, String... children) {
            return "{\"label\": \"" + name + "\", \"name\": \"" + name + "\", \"type\": \"" + type +
                "\", \"parameters\": {}, \"clusterElements\": {\"" + key + "\": [" + String.join(",", children) +
                "]}}";
        }
    }

    private static String validate(String workflow) {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateWorkflow(
            workflow, (taskType, kind) -> null, (taskType, kind, warningsBuilder) -> null, taskType -> null,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        return warnings.toString();
    }

    private static String validateErrors(String workflow) {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateWorkflow(
            workflow, (taskType, kind) -> null, (taskType, kind, warningsBuilder) -> null, taskType -> null,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        return errors.toString();
    }
}
