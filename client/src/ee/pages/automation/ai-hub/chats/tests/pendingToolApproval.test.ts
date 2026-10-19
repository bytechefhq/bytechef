import {type AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {type ThreadMessageLike} from '@assistant-ui/react';
import {describe, expect, it} from 'vitest';

import {
    canResolveToolApproval,
    hasPendingToolApproval,
    stampToolApprovalResolution,
    supersedePendingToolApprovals,
    toToolApprovalConflictResolution,
} from '../pendingToolApproval';

function buildChat(overrides: Partial<AiHubChatI> = {}): AiHubChatI {
    return {
        autoTitled: true,
        createdAt: new Date().toISOString(),
        environmentId: 0,
        id: 5,
        isOwner: true,
        lastPreview: null,
        messageCount: 0,
        ownerUserId: 1,
        status: 'ACTIVE',
        threadId: 'thread-5',
        title: 'My chat',
        updatedAt: new Date().toISOString(),
        userId: 1,
        workspaceId: 7,
        ...overrides,
    };
}

function approvalMessage(data: Record<string, unknown>): ThreadMessageLike {
    return {
        content: [{data, type: 'data-tool-approval-request'}],
        role: 'assistant',
    } as ThreadMessageLike;
}

describe('hasPendingToolApproval', () => {
    it('is false for an empty transcript', () => {
        expect(hasPendingToolApproval([])).toBe(false);
    });

    it('is false for a transcript of ordinary text messages', () => {
        expect(
            hasPendingToolApproval([
                {content: 'hi', role: 'user'},
                {content: 'hello', role: 'assistant'},
            ])
        ).toBe(false);
    });

    it('is true for a card that is still awaiting a decision', () => {
        expect(hasPendingToolApproval([approvalMessage({approvalId: 1, awaitingApproval: true})])).toBe(true);
    });

    it('is false once a decision has been stitched onto the card', () => {
        expect(
            hasPendingToolApproval([
                approvalMessage({approvalId: 1, awaitingApproval: true, resolvedStatus: 'APPROVED'}),
            ])
        ).toBe(false);
    });

    it('is false for a card that never awaited a decision', () => {
        expect(hasPendingToolApproval([approvalMessage({approvalId: 1, awaitingApproval: false})])).toBe(false);
    });

    it('finds a pending card several messages back, since the chat can keep going while it waits', () => {
        expect(
            hasPendingToolApproval([
                {content: 'hi', role: 'user'},
                approvalMessage({approvalId: 1, awaitingApproval: true}),
                {content: 'anything else?', role: 'assistant'},
            ])
        ).toBe(true);
    });

    it('ignores other interactive data parts', () => {
        const askUserQuestion = {
            content: [{data: {awaitingAnswer: true}, type: 'data-ask-user-question'}],
            role: 'assistant',
        } as ThreadMessageLike;

        expect(hasPendingToolApproval([askUserQuestion])).toBe(false);
    });
});

describe('canResolveToolApproval', () => {
    it('is true for the chat owner', () => {
        expect(canResolveToolApproval(buildChat({isOwner: true}))).toBe(true);
    });

    it('is false for a caller who does not own the chat', () => {
        expect(canResolveToolApproval(buildChat({isOwner: false}))).toBe(false);
    });

    it('is false when there is no chat', () => {
        expect(canResolveToolApproval(undefined)).toBe(false);
    });
});

describe('stampToolApprovalResolution', () => {
    it('stamps the decided status onto the matching approval part only', () => {
        const messages = [
            approvalMessage({approvalId: 1, awaitingApproval: true}),
            approvalMessage({approvalId: 2, awaitingApproval: true}),
        ];

        const stampedMessages = stampToolApprovalResolution(messages, 2, {executionError: null, status: 'REJECTED'});

        expect(stampedMessages[0]).toBe(messages[0]);
        expect((stampedMessages[1].content as unknown as Array<{data: Record<string, unknown>}>)[0].data).toEqual({
            approvalId: 2,
            awaitingApproval: true,
            executionError: undefined,
            resolvedStatus: 'REJECTED',
        });
        expect(hasPendingToolApproval(stampedMessages)).toBe(true);
        expect(hasPendingToolApproval(stampToolApprovalResolution(stampedMessages, 1, {status: 'APPROVED'}))).toBe(
            false
        );
    });

    it('carries the execution error of a call that failed after approval', () => {
        const stampedMessages = stampToolApprovalResolution(
            [approvalMessage({approvalId: 3, awaitingApproval: true})],
            3,
            {executionError: 'Workflow not found', status: 'FAILED'}
        );

        expect((stampedMessages[0].content as unknown as Array<{data: Record<string, unknown>}>)[0].data).toMatchObject(
            {
                executionError: 'Workflow not found',
                resolvedStatus: 'FAILED',
            }
        );
    });

    it('leaves the transcript untouched for a PENDING status', () => {
        const messages = [approvalMessage({approvalId: 1, awaitingApproval: true})];

        expect(stampToolApprovalResolution(messages, 1, {status: 'PENDING'})).toBe(messages);
    });
});

describe('supersedePendingToolApprovals', () => {
    const approvalData = (message: ThreadMessageLike) =>
        (message.content as unknown as Array<{data: Record<string, unknown>}>)[0].data;

    it('stamps the approval still awaiting a decision as superseded and leaves decided ones as they are', () => {
        const messages = [
            {content: 'Delete the Demo project', role: 'user'} as ThreadMessageLike,
            approvalMessage({approvalId: 1, awaitingApproval: true, resolvedStatus: 'REJECTED'}),
            approvalMessage({approvalId: 2, awaitingApproval: true}),
        ];

        const supersededMessages = supersedePendingToolApprovals(messages);

        expect(supersededMessages[0]).toBe(messages[0]);
        expect(supersededMessages[1]).toBe(messages[1]);
        expect(approvalData(supersededMessages[2])).toMatchObject({approvalId: 2, resolvedStatus: 'SUPERSEDED'});
        expect(hasPendingToolApproval(supersededMessages)).toBe(false);
    });

    it('returns the same transcript when no approval is waiting', () => {
        const messages = [approvalMessage({approvalId: 1, awaitingApproval: true, resolvedStatus: 'APPROVED'})];

        expect(supersedePendingToolApprovals(messages)).toBe(messages);
    });
});

describe('stampToolApprovalResolution resolution note', () => {
    it('carries the note that explains why the approval can no longer be decided', () => {
        const stampedMessages = stampToolApprovalResolution(
            [approvalMessage({approvalId: 4, awaitingApproval: true})],
            4,
            {
                resolutionNote: 'This approval can no longer be decided: Approval 4 is already SUPERSEDED',
                status: 'SUPERSEDED',
            }
        );

        expect((stampedMessages[0].content as unknown as Array<{data: Record<string, unknown>}>)[0].data).toMatchObject(
            {
                resolutionNote: 'This approval can no longer be decided: Approval 4 is already SUPERSEDED',
                resolvedStatus: 'SUPERSEDED',
            }
        );
    });
});

describe('toToolApprovalConflictResolution', () => {
    it('keeps an approval already approved elsewhere as approved', () => {
        expect(toToolApprovalConflictResolution('Approval 4 is already APPROVED')).toEqual({
            resolutionNote: 'This approval was already approved elsewhere.',
            status: 'APPROVED',
        });
    });

    it('keeps an approval already rejected elsewhere as rejected', () => {
        expect(toToolApprovalConflictResolution('Approval 4 is already REJECTED')).toEqual({
            resolutionNote: 'This approval was already rejected elsewhere.',
            status: 'REJECTED',
        });
    });

    it('keeps an approval whose tool already failed as failed', () => {
        expect(toToolApprovalConflictResolution('Approval 4 is already FAILED')).toEqual({
            resolutionNote: 'This approval was already approved elsewhere, and the tool failed.',
            status: 'FAILED',
        });
    });

    it('marks an approval that has expired as expired, whether it expired now or before', () => {
        const expiredResolution = {
            resolutionNote: 'This approval expired before it was decided.',
            status: 'EXPIRED',
        };

        expect(toToolApprovalConflictResolution('Approval 4 has expired')).toEqual(expiredResolution);
        expect(toToolApprovalConflictResolution('Approval 4 is already EXPIRED')).toEqual(expiredResolution);
    });

    it('marks an approval already superseded as superseded', () => {
        expect(toToolApprovalConflictResolution('Approval 4 is already SUPERSEDED')).toEqual({
            resolutionNote: 'This approval was superseded.',
            status: 'SUPERSEDED',
        });
    });

    it('marks an approval whose tool call was dropped as superseded and says why', () => {
        expect(
            toToolApprovalConflictResolution(
                'Approval 4 no longer matches a tool call in this chat; it was edited or reloaded'
            )
        ).toEqual({
            resolutionNote: 'This approval no longer applies: its message was edited or reloaded.',
            status: 'SUPERSEDED',
        });
    });

    it('falls back to superseded with the server reason for an unknown conflict', () => {
        expect(toToolApprovalConflictResolution('Something else changed')).toEqual({
            resolutionNote: 'This approval can no longer be decided: Something else changed',
            status: 'SUPERSEDED',
        });
    });
});
