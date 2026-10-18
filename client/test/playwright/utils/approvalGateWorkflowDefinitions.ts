import {OPEN_AI_MODEL, SYNC_WEBHOOK_CSRF_TOKEN, buildApprovalChannel} from './approvalWorkflowDefinitions';
import {type WorkflowDefinitionType} from './deploymentUtils';
import {MAILPIT_URL} from './mailpitUtils';

export const GATE_SYSTEM_PROMPT =
    "You are a test assistant. When the user asks to send a notification, call the send_notification_email tool exactly once per requested notification, using the exact subject the user gives. When the user asks about the mail server, call get_mail_server_info. After a tool result, reply briefly and report exactly what the tool returned (including any email ID, any denial and the reviewer's comment).";

export type GateTriggerType = 'chat' | 'synchronousWebhook' | 'webhook';

export function buildSendNotificationTool(recipient: string, nodeName = 'httpClient_1'): Record<string, unknown> {
    return {
        label: 'Send notification email',
        name: nodeName,
        parameters: {
            body: {
                bodyContent: {
                    From: {Email: 'gated-agent@example.com'},
                    Subject: "=fromAi('subject', 'string', 'Exact subject of the notification email')",
                    Text: 'sent by the gated tool',
                    To: [{Email: recipient}],
                },
                bodyContentType: 'JSON',
            },
            toolDescription: 'Sends a notification email with the given subject.',
            toolName: 'send_notification_email',
            uri: `${MAILPIT_URL}/api/v1/send`,
        },
        type: 'httpClient/v1/post',
    };
}

export function buildMailServerInfoTool(nodeName = 'httpClient_2'): Record<string, unknown> {
    return {
        label: 'Mail server info',
        name: nodeName,
        parameters: {
            toolDescription: 'Returns information about the mail server (version, message counts).',
            toolName: 'get_mail_server_info',
            uri: `${MAILPIT_URL}/api/v1/info`,
        },
        type: 'httpClient/v1/get',
    };
}

export interface ApprovalGateI {
    approvalChannels?: Record<string, unknown>[];
    expiresIn?: number;
    expiresInUnit?: 'DAYS' | 'HOURS';
    label: string;
    name: string;
    nodeName?: string;
    tools: Record<string, unknown>[];
}

export function buildApprovalGate({
    approvalChannels,
    expiresIn,
    expiresInUnit,
    label,
    name,
    nodeName = 'approvalGate_1',
    tools,
}: ApprovalGateI): Record<string, unknown> {
    return {
        clusterElements: {...(approvalChannels ? {approvalChannels} : {}), tools},
        label,
        name: nodeName,
        parameters: {
            name,
            ...(expiresIn ? {approvalExpiresIn: expiresIn, approvalExpiresInUnit: expiresInUnit} : {}),
        },
        type: 'aiAgentUtils/v1/approvalGate',
    };
}

export function buildEmailApprovalChannel(recipient: string, mailSubject: string): Record<string, unknown> {
    return {...buildApprovalChannel('email', {mailSubject, recipient}), label: 'Email', name: 'emailChannel_1'};
}

export function buildApprovalTaskChannel(): Record<string, unknown> {
    return {...buildApprovalChannel('approvalTask', {}), label: 'Approval Task', name: 'approvalTaskChannel_1'};
}

export function buildChatChannel(): Record<string, unknown> {
    return {...buildApprovalChannel('chat', {}), label: 'Chat', name: 'chatChannel_1'};
}

/**
 * A trigger, then an AI Agent (gpt-4o-mini) holding the given tools (approval gates and plain tools), then a reply
 * for chat and synchronous-webhook triggers.
 */
export function buildGatedAgentWorkflow({
    label,
    tools,
    trigger,
}: {
    label: string;
    tools: Record<string, unknown>[];
    trigger: GateTriggerType;
}): WorkflowDefinitionType {
    const agentTask = {
        clusterElements: {model: OPEN_AI_MODEL, tools},
        label: 'AI Agent',
        name: 'aiAgent_1',
        parameters: {
            format: 'SIMPLE',
            response: {responseFormat: 'TEXT'},
            systemPrompt: GATE_SYSTEM_PROMPT,
            userPrompt: trigger === 'chat' ? '${trigger_1.message}' : '${trigger_1.body.message}',
        },
        type: 'aiAgent/v1/chat',
    };

    const triggers: Record<GateTriggerType, Record<string, unknown>> = {
        chat: {name: 'trigger_1', parameters: {mode: 1}, type: 'chat/v1/newChatRequest'},
        synchronousWebhook: {
            name: 'trigger_1',
            parameters: {csrfToken: SYNC_WEBHOOK_CSRF_TOKEN, timeout: 60000},
            type: 'webhook/v1/awaitWorkflowAndRespond',
        },
        webhook: {name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'},
    };

    const replyTasks: Record<GateTriggerType, Record<string, unknown>[]> = {
        chat: [{name: 'chat_1', parameters: {message: '${aiAgent_1}'}, type: 'chat/v1/responseToRequest'}],
        synchronousWebhook: [
            {
                name: 'respond_1',
                parameters: {body: {answer: '${aiAgent_1}'}, responseType: 'JSON'},
                type: 'webhook/v1/responseToWebhookRequest',
            },
        ],
        webhook: [],
    };

    return {
        description: label,
        label,
        tasks: [agentTask, ...replyTasks[trigger]],
        triggers: [triggers[trigger]],
    };
}

function buildSubagentTaskTool(subagentTools: Record<string, unknown>[], instructions: string) {
    return {
        clusterElements: {
            model: {...OPEN_AI_MODEL, name: 'model_2'},
            subagent: [
                {
                    clusterElements: {model: {...OPEN_AI_MODEL, name: 'model_3'}, tools: subagentTools},
                    label: 'Notifier',
                    name: 'subagentTool_1',
                    parameters: {description: 'Sends notification emails.', instructions, subagentName: 'notifier'},
                    type: 'aiAgentUtils/v1/subagentTool',
                },
            ],
        },
        label: 'Task Tool',
        name: 'taskTool_1',
        parameters: {},
        type: 'aiAgentUtils/v1/taskTool',
    };
}

/**
 * The three invalid placements of C10: (a) a gate under a subagent, (b) Request Approval inside a gate and (c)
 * Request Approval as a subagent's tool.
 */
export function buildInvalidPlacementTools(placement: 'a' | 'b' | 'c', recipient: string): Record<string, unknown>[] {
    if (placement === 'a') {
        return [
            buildSubagentTaskTool(
                [
                    buildApprovalGate({
                        approvalChannels: [buildApprovalTaskChannel()],
                        label: 'Mail gate',
                        name: 'C10 gate',
                        tools: [buildSendNotificationTool(recipient)],
                    }),
                ],
                'Call send_notification_email once with the exact subject requested and report the result.'
            ),
        ];
    }

    const requestApprovalTool = {
        clusterElements: {approvalChannels: [buildApprovalTaskChannel()]},
        label: 'Request Approval',
        name: 'requestApproval_1',
        parameters: {
            formTitle: 'Nested approval',
            toolDescription: 'Ask a human to approve sending the notification.',
            toolName: 'requestApproval',
        },
        type: 'approval/v1/requestApproval',
    };

    if (placement === 'b') {
        return [
            buildApprovalGate({
                approvalChannels: [buildApprovalTaskChannel()],
                label: 'Mail gate',
                name: 'C10 gate',
                tools: [buildSendNotificationTool(recipient), requestApprovalTool],
            }),
        ];
    }

    return [
        buildSubagentTaskTool(
            [requestApprovalTool, buildSendNotificationTool(recipient)],
            'Before sending, call requestApproval to ask for approval; then call send_notification_email once with the exact subject requested and report the result.'
        ),
    ];
}
