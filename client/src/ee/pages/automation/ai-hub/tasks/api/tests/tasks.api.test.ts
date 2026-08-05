import {AiHubTaskKind, AiHubTaskStatus} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {toTask} from '../tasks.api';

/**
 * Pin the GraphQL → AiHubTaskI mapping. The shape of `aiHubPersonalAgentId`, `workflowExecutionId`,
 * and the kind discriminator have to round-trip through this function for the sidebar, runtime provider, and
 * routing logic to all see the same data. A regression here is silent — the row renders with a wrong icon or
 * navigates to the wrong route — so explicit pinning of the boundary cases catches it at compile time.
 */
describe('toTask', () => {
    const baseGraphQlTask = {
        aiHubPersonalAgentId: null,
        autoTitled: false,
        createdAt: '1716000000000',
        environmentId: '0',
        id: '101',
        kind: 'STANDARD' as unknown as AiHubTaskKind,
        lastPreview: null,
        messageCount: 5,
        projectDeploymentId: null,
        status: AiHubTaskStatus.Active,
        threadId: 'thread-1',
        title: 'Hello',
        updatedAt: '1716000060000',
        userId: '3',
        workflowExecutionId: null,
        workspaceId: '7',
    };

    it('maps aiHubPersonalAgentId from string to number when present', () => {
        const result = toTask({
            ...baseGraphQlTask,
            aiHubPersonalAgentId: '42',
            kind: AiHubTaskKind.PersonalAgent,
        });

        // GraphQL Long → number coercion. The sidebar and runtime provider both compare aiHubPersonalAgentId as a
        // number; if this stayed as a string, the comparisons would silently fail (=== returns false for
        // '42' === 42).
        expect(result.aiHubPersonalAgentId).toBe(42);
        expect(typeof result.aiHubPersonalAgentId).toBe('number');
        expect(result.kind).toBe('PERSONAL_AGENT');
    });

    it('maps aiHubPersonalAgentId to null when absent', () => {
        const result = toTask({
            ...baseGraphQlTask,
            aiHubPersonalAgentId: null,
            kind: 'STANDARD' as unknown as AiHubTaskKind,
        });

        // STANDARD and WORKFLOW_CHAT tasks leave aiHubPersonalAgentId null. Pin null (not undefined) so the
        // sidebar's `aiHubPersonalAgentId == null` check (loose equality) and `=== null` checks both work
        // identically.
        expect(result.aiHubPersonalAgentId).toBeNull();
    });

    it('coerces unknown kind values to STANDARD for forward-compat', () => {
        const result = toTask({
            ...baseGraphQlTask,
            // Simulate a future AiHubTaskKind value the client doesn't recognise yet. Cast through
            // unknown because the codegen enum's stricter type would otherwise reject the synthetic value
            // — the runtime mapping must still tolerate it so we cast at the test boundary.
            kind: 'FUTURE_KIND' as unknown as AiHubTaskKind,
        });

        // Forward-compat: a future kind addition on the server doesn't crash the sidebar render. Falling back
        // to STANDARD is the safe default — the user sees an unflavoured row instead of a runtime
        // exception.
        expect(result.kind).toBe('STANDARD');
    });

    it('preserves PERSONAL_AGENT kind round-trip', () => {
        const result = toTask({
            ...baseGraphQlTask,
            aiHubPersonalAgentId: '42',
            kind: AiHubTaskKind.PersonalAgent,
        });

        // Pin the kind discriminator: the sidebar's BotIcon branch and the runtime provider's overlay
        // injection both depend on this value being exactly 'PERSONAL_AGENT'. A typo or coercion regression
        // would silently drop PA tasks into the STANDARD visual bucket.
        expect(result.kind).toBe('PERSONAL_AGENT');
    });

    it('preserves WORKFLOW_CHAT kind round-trip', () => {
        const result = toTask({
            ...baseGraphQlTask,
            kind: AiHubTaskKind.WorkflowChat,
            workflowExecutionId: 'exec-1',
        });

        expect(result.kind).toBe('WORKFLOW_CHAT');
        expect(result.workflowExecutionId).toBe('exec-1');
    });
});
