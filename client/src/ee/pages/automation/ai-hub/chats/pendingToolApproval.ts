import {type AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {type ThreadMessageLike} from '@assistant-ui/react';

/**
 * Whether this chat's transcript holds a gated tool call nobody has decided yet.
 *
 * <p>The approval IS a message: a {@code data-tool-approval-request} part on an assistant row, put there live by the
 * runtime provider or on chat load by {@code useSwitchChat}. {@code resolvedStatus} is stitched onto a decided part
 * on chat load and stamped live by {@link stampToolApprovalResolution} when this session decides it. A part that still says {@code awaitingApproval} and carries no
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

export interface ToolApprovalResolutionI {
    executionError?: string | null;
    status: string;
}

/**
 * Returns {@code messages} with {@code resolution} stamped onto the {@code data-tool-approval-request} part of
 * {@code approvalId}, in the same shape {@code useSwitchChat} stitches on chat load. A {@code PENDING} status and
 * messages without that part are left as they are.
 */
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

/**
 * Whether the caller could decide a pending approval on {@code chat}. Approvals are resolved by the chat owner.
 */
export function canResolveToolApproval(chat: AiHubChatI | undefined): boolean {
    return chat?.isOwner === true;
}
