import {type AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {type ThreadMessageLike} from '@assistant-ui/react';

export function hasPendingToolApproval(messages: ThreadMessageLike[]): boolean {
    return messages.some((message) => {
        if (message.role !== 'assistant' || !Array.isArray(message.content)) {
            return false;
        }

        return message.content.some((part) => {
            if (
                typeof part !== 'object' ||
                part === null ||
                (part as {type?: unknown}).type !== 'data-tool-approval-request'
            ) {
                return false;
            }

            const data = (part as {data?: Record<string, unknown>}).data;

            return data?.awaitingApproval === true && data.resolvedStatus == null;
        });
    });
}

export interface ToolApprovalResolutionI {
    executionError?: string | null;
    status: string;
}

export function stampToolApprovalResolution(
    messages: ThreadMessageLike[],
    approvalId: number,
    resolution: ToolApprovalResolutionI
): ThreadMessageLike[] {
    if (resolution.status === 'PENDING') {
        return messages;
    }

    return messages.map((message) => {
        if (message.role !== 'assistant' || !Array.isArray(message.content)) {
            return message;
        }

        let changed = false;

        const content = message.content.map((part) => {
            if (
                typeof part !== 'object' ||
                part === null ||
                (part as {type?: unknown}).type !== 'data-tool-approval-request'
            ) {
                return part;
            }

            const data = (part as {data: Record<string, unknown>}).data;

            if (data?.approvalId !== approvalId) {
                return part;
            }

            changed = true;

            return {
                ...part,
                data: {
                    ...data,
                    executionError: resolution.executionError ?? undefined,
                    resolvedStatus: resolution.status,
                },
            };
        });

        return changed ? ({...message, content} as ThreadMessageLike) : message;
    });
}

export function canResolveToolApproval(chat: AiHubChatI | undefined): boolean {
    return chat?.isOwner === true;
}
