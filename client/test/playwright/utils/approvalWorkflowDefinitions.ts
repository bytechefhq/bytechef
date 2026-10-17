import {type WorkflowDefinitionType} from './deploymentUtils';
import {MAILPIT_URL} from './mailpitUtils';

export type ApprovalChannelType =
    'approvalTask' | 'chat' | 'discord' | 'email' | 'mattermost' | 'slack' | 'telegram' | 'whatsApp';

export const ALL_IN_PLACE_CHANNELS: ApprovalChannelType[] = [
    'email',
    'slack',
    'telegram',
    'whatsApp',
    'mattermost',
    'discord',
    'approvalTask',
];

export const APPROVAL_SUMMARY_TEMPLATE =
    'approved=${approval_1.approved} by=${approval_1.approvedBy} comment=${approval_1.comment}';

export interface ApprovalFormInputI {
    fieldLabel: string;
    fieldName: string;
    fieldType: number;
    required?: boolean;
}

export const TEXT_INPUT_FIELD_TYPE = 6;

export const NUMBER_INPUT_FIELD_TYPE = 9;

export function buildApprovalChannel(
    channel: ApprovalChannelType,
    {mailSubject, recipient}: {mailSubject?: string; recipient?: string}
): Record<string, unknown> {
    const channelParameters: Record<ApprovalChannelType, Record<string, unknown>> = {
        approvalTask: {},
        chat: {},
        discord: {channelId: '2', guildId: '1'},
        email: {
            from: 'bot@example.com',
            port: 1025,
            subject: mailSubject ?? 'Approval request',
            to: [recipient ?? 'approver@example.com'],
        },
        mattermost: {channel_id: 'chan1'},
        slack: {channel: 'C0123456789'},
        telegram: {chat_id: '12345'},
        whatsApp: {to: '385911234567'},
    };

    return {
        name: `${channel}_1`,
        parameters: channelParameters[channel],
        type: `${channel}/v1/${channel}`,
    };
}

interface ApprovalWorkflowProps {
    channels: ApprovalChannelType[];
    expiresIn?: number;
    expiresInUnit?: 'DAYS' | 'HOURS';
    formDescription?: string;
    formTitle: string;
    inputs?: ApprovalFormInputI[];
    label: string;
    mailSubject?: string;
    nextStepValue?: string | Record<string, string>;
    recipient?: string;
}

/**
 * Webhook trigger, then Request Approval with the given channels, then a Set variable step that renders the
 * decision (the shape of the A and B flows).
 */
export function buildApprovalWorkflow({
    channels,
    expiresIn = 2,
    expiresInUnit = 'HOURS',
    formDescription = 'Please approve ${trigger_1.body.n}',
    formTitle,
    inputs,
    label,
    mailSubject,
    nextStepValue = APPROVAL_SUMMARY_TEMPLATE,
    recipient,
}: ApprovalWorkflowProps): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                clusterElements: {
                    approvalChannels: channels.map((channel) =>
                        buildApprovalChannel(channel, {mailSubject, recipient})
                    ),
                },
                name: 'approval_1',
                parameters: {
                    expiresIn,
                    expiresInUnit,
                    formDescription,
                    formTitle,
                    ...(inputs ? {inputs} : {}),
                },
                type: 'approval/v1/requestApproval',
            },
            {
                name: 'var_1',
                parameters:
                    typeof nextStepValue === 'string'
                        ? {type: 'STRING', value: nextStepValue}
                        : {type: 'OBJECT', value: nextStepValue},
                type: 'var/v1/set',
            },
        ],
        triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'}],
    };
}

export function buildChatApprovalWorkflow(label: string): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                clusterElements: {approvalChannels: [buildApprovalChannel('chat', {})]},
                name: 'approval_1',
                parameters: {
                    expiresIn: 2,
                    expiresInUnit: 'HOURS',
                    formDescription: '${trigger_1.message}',
                    formTitle: 'Approval for ${trigger_1.message}',
                },
                type: 'approval/v1/requestApproval',
            },
            {
                name: 'respond_1',
                parameters: {
                    message:
                        'Decision for ${trigger_1.message}: approved=${approval_1.approved} comment=${approval_1.comment}',
                },
                type: 'chat/v1/responseToRequest',
            },
        ],
        triggers: [{name: 'trigger_1', parameters: {mode: 1}, type: 'chat/v1/newChatRequest'}],
    };
}

export const SYNC_WEBHOOK_CSRF_TOKEN = 'playwright-csrf';

export function buildSyncApprovalWorkflow(label: string): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                clusterElements: {approvalChannels: [buildApprovalChannel('approvalTask', {})]},
                name: 'approval_1',
                parameters: {expiresIn: 2, expiresInUnit: 'HOURS', formTitle: label},
                type: 'approval/v1/requestApproval',
            },
            {
                name: 'respond_1',
                parameters: {body: {approved: '${approval_1.approved}'}, responseType: 'JSON'},
                type: 'webhook/v1/responseToWebhookRequest',
            },
        ],
        triggers: [
            {
                name: 'trigger_1',
                parameters: {csrfToken: SYNC_WEBHOOK_CSRF_TOKEN, timeout: 60000},
                type: 'webhook/v1/awaitWorkflowAndRespond',
            },
        ],
    };
}

export function buildPlainWaitWorkflow(label: string, serviceUrl: string): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                name: 'wait_1',
                parameters: {amount: 2, serviceUrl, unit: 'HOURS'},
                type: 'wait/v1/onWebhookCall',
            },
            {name: 'var_1', parameters: {type: 'OBJECT', value: {wait: '${wait_1}'}}, type: 'var/v1/set'},
        ],
        triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'}],
    };
}

export function buildPlainTimeWaitWorkflow(label: string): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [{name: 'wait_1', parameters: {amount: 2, unit: 'HOURS'}, type: 'wait/v1/afterTimeInterval'}],
        triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'}],
    };
}

/**
 * A callable workflow (New Workflow Call trigger) that asks for approval and returns the decision to its caller.
 */
export function buildCallableApprovalWorkflow({
    channels,
    label,
    recipient,
}: {
    channels: ApprovalChannelType[];
    label: string;
    recipient?: string;
}): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                clusterElements: {
                    approvalChannels: channels.map((channel) =>
                        buildApprovalChannel(channel, {mailSubject: `${label} mail \${trigger_1.item}`, recipient})
                    ),
                },
                name: 'approval_1',
                parameters: {
                    expiresIn: 2,
                    expiresInUnit: 'HOURS',
                    formDescription: 'Child asks to order ${trigger_1.item}',
                    formTitle: `${label} for \${trigger_1.item}`,
                },
                type: 'approval/v1/requestApproval',
            },
            {
                name: 'responseToWorkflowCall_1',
                parameters: {
                    outputSchema: JSON.stringify({
                        properties: {approved: {type: 'boolean'}, comment: {type: 'string'}, item: {type: 'string'}},
                        type: 'object',
                    }),
                    response: {
                        approved: '${approval_1.approved}',
                        comment: '${approval_1.comment}',
                        item: '${trigger_1.item}',
                    },
                },
                type: 'workflow/v1/responseToWorkflowCall',
            },
        ],
        triggers: [
            {
                name: 'trigger_1',
                parameters: {
                    inputSchema: JSON.stringify({
                        properties: {item: {description: 'What to approve', type: 'string'}},
                        type: 'object',
                    }),
                },
                type: 'workflow/v1/newWorkflowCall',
            },
        ],
    };
}

export const OPEN_AI_MODEL = {
    name: 'model_1',
    parameters: {model: 'gpt-4o-mini', temperature: 0},
    type: 'openAi/v1/model',
};

const ORDER_SYSTEM_PROMPT =
    'You are a purchasing assistant. When asked to order an item, call the order_item tool once with the item name, then report exactly what it returned (approved flag, comment, item, or the error text).';

/**
 * Webhook (or synchronous webhook) trigger, then an AI Agent whose `order_item` tool calls the given child
 * workflow (the D1 and D2 parents).
 */
export function buildAgentCallWorkflowParent({
    childWorkflowUuid,
    label,
    synchronous = false,
}: {
    childWorkflowUuid: string;
    label: string;
    synchronous?: boolean;
}): WorkflowDefinitionType {
    const agentTask = {
        clusterElements: {
            model: OPEN_AI_MODEL,
            tools: [
                {
                    name: 'callWorkflow_1',
                    parameters: {
                        inputs: {item: "=fromAi('item','string','Name of the item to order')"},
                        toolDescription: 'Orders an item; a human must approve the order.',
                        toolName: 'order_item',
                        workflowUuid: childWorkflowUuid,
                    },
                    type: 'workflow/v1/callWorkflow',
                },
            ],
        },
        name: 'aiAgent_1',
        parameters: {
            format: 'SIMPLE',
            response: {responseFormat: 'TEXT'},
            systemPrompt: ORDER_SYSTEM_PROMPT,
            userPrompt: '${trigger_1.body.message}',
        },
        type: 'aiAgent/v1/chat',
    };

    if (synchronous) {
        return {
            description: label,
            label,
            tasks: [
                agentTask,
                {
                    name: 'respond_1',
                    parameters: {body: {answer: '${aiAgent_1}'}, responseType: 'JSON'},
                    type: 'webhook/v1/responseToWebhookRequest',
                },
            ],
            triggers: [
                {
                    name: 'trigger_1',
                    parameters: {csrfToken: SYNC_WEBHOOK_CSRF_TOKEN, timeout: 120000},
                    type: 'webhook/v1/awaitWorkflowAndRespond',
                },
            ],
        };
    }

    return {
        description: label,
        label,
        tasks: [agentTask],
        triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'}],
    };
}

export function buildSubflowParent({
    childWorkflowUuid,
    item,
    label,
}: {
    childWorkflowUuid: string;
    item: string;
    label: string;
}): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                name: 'subflow_1',
                parameters: {inputs: {item}, workflowUuid: childWorkflowUuid},
                type: 'subflow/v1',
            },
            {name: 'var_1', parameters: {type: 'STRING', value: 'child said ${subflow_1}'}, type: 'var/v1/set'},
        ],
        triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'}],
    };
}

export function buildMailpitSendTool({recipient, text}: {recipient: string; text: string}): Record<string, unknown> {
    return {
        name: 'httpClient_1',
        parameters: {
            body: {
                bodyContent: {
                    From: {Email: 'agent@example.com'},
                    Subject: "=fromAi('subject', 'string', 'Exact subject of the notification email')",
                    Text: text,
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

/**
 * Chat trigger, then an AI Agent whose tools are the Request Approval tool (Chat channel, no gate) and an HTTP
 * POST to Mailpit's send API.
 */
export function buildAgentApprovalToolWorkflow({
    formTitle,
    label,
    recipient,
}: {
    formTitle: string;
    label: string;
    recipient: string;
}): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                clusterElements: {
                    model: OPEN_AI_MODEL,
                    tools: [
                        {
                            clusterElements: {approvalChannels: [buildApprovalChannel('chat', {})]},
                            name: 'requestApproval_1',
                            parameters: {
                                expiresIn: 2,
                                expiresInUnit: 'HOURS',
                                formDescription: 'The agent wants to send a notification email',
                                formTitle,
                            },
                            type: 'approval/v1/requestApproval',
                        },
                        buildMailpitSendTool({recipient, text: 'sent by the agent after the approval tool'}),
                    ],
                },
                name: 'aiAgent_1',
                parameters: {
                    format: 'SIMPLE',
                    response: {responseFormat: 'TEXT'},
                    systemPrompt:
                        'You are a test assistant. When the user asks to send a notification, you MUST first call the Request Approval tool exactly once. Only if its result says it was approved, call send_notification_email once with the exact subject the user gives. If it was rejected, do not send and tell the user, quoting the reviewer comment. Finally reply briefly with what happened, including the email ID returned by the send tool.',
                    userPrompt: '${trigger_1.message}',
                },
                type: 'aiAgent/v1/chat',
            },
            {name: 'chat_2', parameters: {message: '${aiAgent_1}'}, type: 'chat/v1/responseToRequest'},
        ],
        triggers: [{name: 'trigger_1', parameters: {mode: 1}, type: 'chat/v1/newChatRequest'}],
    };
}

/**
 * Chat trigger, then Request Approval (Chat channel), then a streaming AI Agent reply. The streaming step makes the
 * Chats page talk to the workflow over SSE, which a run that pauses for an approval needs.
 */
export function buildStreamingChatApprovalWorkflow(label: string): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                clusterElements: {approvalChannels: [buildApprovalChannel('chat', {})]},
                name: 'approval_1',
                parameters: {
                    expiresIn: 2,
                    expiresInUnit: 'HOURS',
                    formDescription: '${trigger_1.message}',
                    formTitle: 'Approval for ${trigger_1.message}',
                },
                type: 'approval/v1/requestApproval',
            },
            {
                clusterElements: {model: OPEN_AI_MODEL},
                name: 'aiAgent_1',
                parameters: {
                    format: 'SIMPLE',
                    response: {responseFormat: 'TEXT'},
                    systemPrompt: 'Reply in at most 12 words.',
                    userPrompt:
                        'Tell the user their request was decided: approved=${approval_1.approved}, comment=${approval_1.comment}.',
                },
                type: 'aiAgent/v1/streamChat',
            },
        ],
        triggers: [{name: 'trigger_1', parameters: {mode: 1}, type: 'chat/v1/newChatRequest'}],
    };
}
