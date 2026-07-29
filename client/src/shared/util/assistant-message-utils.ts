import {ThreadMessageLike} from '@assistant-ui/react';

export interface AskUserQuestionOptionI {
    description: string;
    label: string;
}

export interface AskUserQuestionI {
    header: string;
    multiSelect: boolean;
    options: AskUserQuestionOptionI[];
    question: string;
}

export interface AskUserQuestionEventI {
    questions: AskUserQuestionI[];
    resumeUrl?: string;
}

export interface ApprovalRequestEventI {
    expiresAt?: string;
    formDescription?: string;
    formTitle?: string;
    formUrl?: string;
    inputs?: unknown[];
    resumeId: string;
}

export interface ToolExecutionEventI {
    confidence: string;
    inputs: Record<string, unknown>;
    output: unknown;
    reasoning: string;
    toolName: string;
}

type ContentPartType = {type: string; text?: string; [key: string]: unknown};

/**
 * Appends content to the last assistant message in the messages array.
 * If no assistant message exists, creates a new one with the delta content.
 * Supports both string content and content array formats.
 */
export function appendToLastAssistantMessage(messages: ThreadMessageLike[], delta: string): ThreadMessageLike[] {
    const updatedMessages = [...messages];

    for (let i = updatedMessages.length - 1; i >= 0; i--) {
        const message = updatedMessages[i];

        if (!message || message.role !== 'assistant') {
            continue;
        }

        if (Array.isArray(message.content)) {
            const contentArray = [...message.content] as ContentPartType[];
            const lastPart = contentArray[contentArray.length - 1];

            if (lastPart && lastPart.type === 'text') {
                contentArray[contentArray.length - 1] = {...lastPart, text: String(lastPart.text || '') + delta};
            } else {
                contentArray.push({text: delta, type: 'text'});
            }

            updatedMessages[i] = {...message, content: contentArray} as unknown as ThreadMessageLike;
        } else {
            const current = typeof message.content === 'string' ? message.content : '';

            updatedMessages[i] = {...message, content: current + delta};
        }

        return updatedMessages;
    }

    return [...updatedMessages, {content: delta, role: 'assistant'} as ThreadMessageLike];
}

/**
 * Sets the content of the last assistant message in the messages array.
 * If no assistant message exists, creates a new one with the content.
 * Supports both string content and content array formats.
 */
export function setLastAssistantMessageContent(messages: ThreadMessageLike[], content: string): ThreadMessageLike[] {
    const updatedMessages = [...messages];

    for (let i = updatedMessages.length - 1; i >= 0; i--) {
        const message = updatedMessages[i];

        if (!message || message.role !== 'assistant') {
            continue;
        }

        if (Array.isArray(message.content)) {
            const contentArray = [...message.content] as ContentPartType[];
            const textIndex = contentArray.findLastIndex((part) => part.type === 'text');

            if (textIndex >= 0) {
                contentArray[textIndex] = {...contentArray[textIndex], text: content};
            } else {
                contentArray.push({text: content, type: 'text'});
            }

            updatedMessages[i] = {...message, content: contentArray} as unknown as ThreadMessageLike;
        } else {
            updatedMessages[i] = {...message, content};
        }

        return updatedMessages;
    }

    return [...updatedMessages, {content, role: 'assistant'} as ThreadMessageLike];
}

/**
 * Formats an ask_user_question event as a readable assistant message with the questions and options.
 */
export function formatAskUserQuestionMessage(event: AskUserQuestionEventI): string {
    return event.questions
        .map((question) => {
            const optionLines = question.options
                .map((option, index) => `  ${index + 1}. **${option.label}** — ${option.description}`)
                .join('\n');

            return `**${question.header}**: ${question.question}\n${optionLines}`;
        })
        .join('\n\n');
}

/**
 * Formats an approval_request event as a readable assistant message with a link to the hosted approval form. Used by
 * chat surfaces that render plain markdown instead of the interactive approval card — the link resolves the approval
 * on the hosted form page, never through the chat input.
 */
export function formatApprovalRequestMessage(event: ApprovalRequestEventI): string {
    const title = event.formTitle || 'Approval requested';
    const description = event.formDescription ? `\n\n${event.formDescription}` : '';
    const expiry = event.expiresAt ? `\n\nExpires ${new Date(event.expiresAt).toLocaleString()}` : '';
    const link = event.formUrl ? `\n\n[Open the approval form](${event.formUrl})` : '';

    return `**${title}**${description}${expiry}${link}`;
}

/**
 * Adds a tool execution event as a tool-call content part to the last assistant message.
 * Converts the assistant message content to array format if needed.
 */
export function addToolExecutionToLastAssistantMessage(
    messages: ThreadMessageLike[],
    toolExecution: ToolExecutionEventI
): ThreadMessageLike[] {
    const updatedMessages = [...messages];

    for (let i = updatedMessages.length - 1; i >= 0; i--) {
        const message = updatedMessages[i];

        if (!message || message.role !== 'assistant') {
            continue;
        }

        const contentArray = Array.isArray(message.content)
            ? ([...message.content] as ContentPartType[])
            : message.content
              ? [{text: String(message.content), type: 'text'}]
              : [];

        contentArray.push({
            args: toolExecution.inputs,
            result: {
                confidence: toolExecution.confidence,
                output: toolExecution.output,
                reasoning: toolExecution.reasoning,
            },
            toolCallId: `tc_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
            toolName: toolExecution.toolName,
            type: 'tool-call',
        });

        updatedMessages[i] = {...message, content: contentArray} as unknown as ThreadMessageLike;

        return updatedMessages;
    }

    return updatedMessages;
}
