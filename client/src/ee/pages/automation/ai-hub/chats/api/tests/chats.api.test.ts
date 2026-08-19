import {AiHubChatKind, AiHubChatStatus} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {AiHubChatI, getChatDisplayTitle, isChannelAgentChat, toChat} from '../chats.api';

/**
 * Pin the GraphQL → AiHubChatI mapping. The shape of `aiAgentId`, `workflowExecutionId`, and the kind
 * discriminator have to round-trip through this function for the sidebar, runtime provider, and
 * routing logic to all see the same data. A regression here is silent — the row renders with a wrong icon or
 * navigates to the wrong route — so explicit pinning of the boundary cases catches it at compile time.
 */
describe('toChat', () => {
    const baseGraphQlChat = {
        aiAgentId: null,
        autoTitled: false,
        createdAt: '1716000000000',
        environmentId: '0',
        id: '101',
        kind: 'STANDARD' as unknown as AiHubChatKind,
        lastPreview: null,
        messageCount: 5,
        projectDeploymentId: null,
        status: AiHubChatStatus.Active,
        threadId: 'thread-1',
        title: 'Hello',
        updatedAt: '1716000060000',
        userId: '3',
        workflowExecutionId: null,
        workspaceId: '7',
    };

    it('maps aiAgentId from string to number when present', () => {
        const result = toChat({
            ...baseGraphQlChat,
            aiAgentId: '55',
            kind: AiHubChatKind.AgentChat,
        });

        // GraphQL Long → number coercion. isChannelAgentChat compares aiAgentId with
        // `!= null`, which tolerates a string just as well as a number, but every OTHER consumer (id
        // equality against a fetched AiAgent, React key stability) needs the number.
        expect(result.aiAgentId).toBe(55);
        expect(typeof result.aiAgentId).toBe('number');
    });

    it('maps aiAgentId to null when absent', () => {
        const result = toChat({...baseGraphQlChat, aiAgentId: null});

        expect(result.aiAgentId).toBeNull();
    });

    it('coerces unknown kind values to STANDARD for forward-compat', () => {
        const result = toChat({
            ...baseGraphQlChat,
            // Simulate an AiHubChatKind value the client doesn't recognise. Cast through unknown because the
            // codegen enum's stricter type would otherwise reject the synthetic value — the runtime mapping
            // must still tolerate it so we cast at the test boundary.
            kind: 'FUTURE_KIND' as unknown as AiHubChatKind,
        });

        // Forward-compat: a kind the client doesn't handle doesn't crash the sidebar render. Falling back to
        // STANDARD is the safe default — the user sees an unflavoured row instead of a runtime exception.
        expect(result.kind).toBe('STANDARD');
    });

    it('coerces the retired TASK kind to STANDARD', () => {
        // The AI Hub tasks surface is gone client-side while the server can still return TASK rows created
        // before the removal. Pin that they land in the STANDARD bucket rather than rendering as an
        // unhandled kind — this is the same fallback the forward-compat case exercises, asserted for the
        // one value we know is still on the wire.
        const result = toChat({
            ...baseGraphQlChat,
            kind: 'TASK' as unknown as AiHubChatKind,
        });

        expect(result.kind).toBe('STANDARD');
    });

    it('preserves WORKFLOW_CHAT kind round-trip', () => {
        const result = toChat({
            ...baseGraphQlChat,
            kind: AiHubChatKind.WorkflowChat,
            workflowExecutionId: 'exec-1',
        });

        expect(result.kind).toBe('WORKFLOW_CHAT');
        expect(result.workflowExecutionId).toBe('exec-1');
    });

    it('preserves AGENT_CHAT kind with a null workflowExecutionId and a non-null aiAgentId (channel-born row)', () => {
        // AiHubAgentConversationRecorder never sets workflow_execution_id — there's no execution to bind
        // to for a chat recorded from an agent's channel (Slack, a schedule, …) — but DOES stamp
        // aiAgentId. Both must round-trip faithfully, not coerce to a string like "null", or every reader
        // downstream (isChannelAgentChat, getChatDisplayTitle, the sidebar icon branch) would
        // misclassify the row.
        const result = toChat({
            ...baseGraphQlChat,
            aiAgentId: '9',
            kind: AiHubChatKind.AgentChat,
            title: null,
            workflowExecutionId: null,
        });

        expect(result.kind).toBe('AGENT_CHAT');
        expect(result.aiAgentId).toBe(9);
        expect(result.workflowExecutionId).toBeNull();
    });
});

/**
 * isChannelAgentChat is the two-signal discriminator the client uses for telling a channel-born AGENT_CHAT
 * row (recorded by AiHubAgentConversationRecorder, which stamps aiAgentId but has no execution to bind
 * workflowExecutionId to) apart from a composer-created one (createAgentChatAiHubChat requires
 * workflowExecutionId but never stamps aiAgentId). It deliberately mirrors the server's own
 * AiHubAgentConversationRecorder#adoptChat guard, which insists on both signals rather than trusting
 * either alone — these tests pin every corner of that 2x2, not just the two "normal" cases, since every
 * affected UI branch (sidebar icon, panel badge, display title) trusts this function exclusively.
 */
describe('isChannelAgentChat', () => {
    it('is true for an AGENT_CHAT row with a non-null aiAgentId and a null workflowExecutionId', () => {
        expect(isChannelAgentChat({aiAgentId: 9, kind: 'AGENT_CHAT', workflowExecutionId: null})).toBe(true);
    });

    it('is false for an AGENT_CHAT row with a non-null aiAgentId but a non-null workflowExecutionId (composer-created, hypothetically stamped)', () => {
        expect(isChannelAgentChat({aiAgentId: 9, kind: 'AGENT_CHAT', workflowExecutionId: 'exec-1'})).toBe(false);
    });

    it("is false for an AGENT_CHAT row with a null aiAgentId even when workflowExecutionId is also null (today's actual composer-created shape)", () => {
        // This is the case the previous single-signal implementation got wrong in principle: a
        // composer-created AGENT_CHAT never stamps aiAgentId, so relying on workflowExecutionId alone
        // happened to work only because nothing else produces that combination today. The two-signal
        // guard is correct by construction instead of by accident.
        expect(isChannelAgentChat({aiAgentId: null, kind: 'AGENT_CHAT', workflowExecutionId: null})).toBe(false);
    });

    it('is false for an AGENT_CHAT row with a null aiAgentId and a non-null workflowExecutionId (composer-created)', () => {
        expect(isChannelAgentChat({aiAgentId: null, kind: 'AGENT_CHAT', workflowExecutionId: 'exec-1'})).toBe(false);
    });

    it('is false for a WORKFLOW_CHAT row even with a non-null aiAgentId and a null workflowExecutionId', () => {
        expect(isChannelAgentChat({aiAgentId: 9, kind: 'WORKFLOW_CHAT', workflowExecutionId: null})).toBe(false);
    });

    it('is false for a STANDARD row', () => {
        expect(isChannelAgentChat({aiAgentId: null, kind: 'STANDARD', workflowExecutionId: null})).toBe(false);
    });
});

describe('getChatDisplayTitle', () => {
    const baseChat: Pick<AiHubChatI, 'aiAgentId' | 'kind' | 'title' | 'workflowExecutionId'> = {
        aiAgentId: null,
        kind: 'STANDARD',
        title: null,
        workflowExecutionId: null,
    };

    it('returns the stored title when present, regardless of kind', () => {
        expect(getChatDisplayTitle({...baseChat, aiAgentId: 9, kind: 'AGENT_CHAT', title: 'Custom title'})).toBe(
            'Custom title'
        );
    });

    it('falls back to "New Chat" for an untitled non-channel row', () => {
        expect(getChatDisplayTitle(baseChat)).toBe('New Chat');
    });

    it('falls back to "New Chat" for an untitled composer-created AGENT_CHAT row', () => {
        expect(
            getChatDisplayTitle({aiAgentId: null, kind: 'AGENT_CHAT', title: null, workflowExecutionId: 'exec-1'})
        ).toBe('New Chat');
    });

    it('falls back to "Agent Conversation" for an untitled channel-born AGENT_CHAT row', () => {
        // The distinguishing label so a busy Slack agent's transcripts don't read as the user's own
        // unstarted drafts in the sidebar.
        expect(getChatDisplayTitle({aiAgentId: 9, kind: 'AGENT_CHAT', title: null, workflowExecutionId: null})).toBe(
            'Agent Conversation'
        );
    });
});
