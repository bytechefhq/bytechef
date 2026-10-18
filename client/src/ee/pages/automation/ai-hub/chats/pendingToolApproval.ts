import {type AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {type ThreadMessageLike} from '@assistant-ui/react';

/**
 * Whether this chat's transcript holds a gated tool call nobody has decided yet.
 *
 * <p>The approval IS a message: a {@code data-tool-approval-request} part on an assistant row, put there live by the
 * runtime provider or on chat load by {@code useSwitchChat}, which also stitches {@code resolvedStatus} onto every
 * part whose approval has since been decided. A part that still says {@code awaitingApproval} and carries no
 * {@code resolvedStatus} is the pending one. The server allows at most one pending approval per chat.</p>
 */
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

/**
 * Whether the caller could decide a pending approval on {@code chat}. Approvals are resolved by the chat owner.
 */
export function canResolveToolApproval(chat: AiHubChatI | undefined): boolean {
    return chat?.isOwner === true;
}
