import {type AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {type ThreadMessageLike} from '@assistant-ui/react';

type ToolApprovalRequestPartType = {data?: Record<string, unknown>; type: 'data-tool-approval-request'};

const findPendingToolApprovalParts = (messages: ThreadMessageLike[]): ToolApprovalRequestPartType[] =>
    messages.flatMap((message) => {
        if (message.role !== 'assistant' || !Array.isArray(message.content)) {
            return [];
        }

        return message.content.filter((part): part is ToolApprovalRequestPartType => {
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

/**
 * Whether this chat's transcript holds a gated tool call nobody has decided yet.
 *
 * <p>The approval IS a message: a {@code data-tool-approval-request} part on an assistant row, put there live by the
 * runtime provider or on chat load by {@code useSwitchChat}. {@code resolvedStatus} is stitched onto a decided part
 * on chat load and stamped live by {@link stampToolApprovalResolution} when this session decides it. A part that still says {@code awaitingApproval} and carries no
 * {@code resolvedStatus} is the pending one. The server allows at most one pending approval per chat.</p>
 */
export function hasPendingToolApproval(messages: ThreadMessageLike[]): boolean {
    return findPendingToolApprovalParts(messages).length > 0;
}

export interface ToolApprovalResolutionI {
    executionError?: string | null;
    resolutionNote?: string | null;
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
                    resolutionNote: resolution.resolutionNote ?? undefined,
                    resolvedStatus: resolution.status,
                },
            };
        });

        return changed ? ({...message, content} as ThreadMessageLike) : message;
    });
}

/**
 * Returns {@code messages} with every approval still awaiting a decision stamped {@code SUPERSEDED}, which is what
 * the server does to them once it accepts a new turn in the chat.
 */
export function supersedePendingToolApprovals(messages: ThreadMessageLike[]): ThreadMessageLike[] {
    const pendingApprovalIds = findPendingToolApprovalParts(messages)
        .map((part) => part.data?.approvalId)
        .filter((approvalId): approvalId is number => typeof approvalId === 'number');

    return pendingApprovalIds.reduce(
        (stampedMessages, approvalId) =>
            stampToolApprovalResolution(stampedMessages, approvalId, {status: 'SUPERSEDED'}),
        messages
    );
}

const ALREADY_DECIDED_PATTERN = /\bis already (APPROVED|EXPIRED|FAILED|REJECTED|SUPERSEDED)\b/;

const ALREADY_DECIDED_NOTES: Record<string, string> = {
    APPROVED: 'This approval was already approved elsewhere.',
    EXPIRED: 'This approval expired before it was decided.',
    FAILED: 'This approval was already approved elsewhere, and the tool failed.',
    REJECTED: 'This approval was already rejected elsewhere.',
    SUPERSEDED: 'This approval was superseded.',
};

/**
 * The status and note for an approval the server refused to resolve with CONFLICT, read from the server's message:
 * {@code is already <STATUS>}, {@code has expired}, or {@code no longer matches a tool call} (its message was edited or
 * reloaded).
 */
export function toToolApprovalConflictResolution(conflictMessage: string): {resolutionNote: string; status: string} {
    const alreadyDecidedStatus = ALREADY_DECIDED_PATTERN.exec(conflictMessage)?.[1];

    if (alreadyDecidedStatus) {
        return {resolutionNote: ALREADY_DECIDED_NOTES[alreadyDecidedStatus], status: alreadyDecidedStatus};
    }

    if (/\bhas expired\b/.test(conflictMessage)) {
        return {resolutionNote: ALREADY_DECIDED_NOTES.EXPIRED, status: 'EXPIRED'};
    }

    if (conflictMessage.includes('no longer matches a tool call')) {
        return {
            resolutionNote: 'This approval no longer applies: its message was edited or reloaded.',
            status: 'SUPERSEDED',
        };
    }

    return {resolutionNote: `This approval can no longer be decided: ${conflictMessage}`, status: 'SUPERSEDED'};
}

/**
 * Whether the caller could decide a pending approval on {@code chat}. Approvals are resolved by the chat owner.
 */
export function canResolveToolApproval(chat: AiHubChatI | undefined): boolean {
    return chat?.isOwner === true;
}
