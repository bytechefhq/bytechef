import {type AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {type ThreadMessageLike} from '@assistant-ui/react';
import {describe, expect, it} from 'vitest';

import {canResolveToolApproval, hasPendingToolApproval} from '../pendingToolApproval';

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
