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

export function canResolveToolApproval(chat: AiHubChatI | undefined): boolean {
    return chat?.isOwner === true;
}
