import {createContext, useContext} from 'react';

export class ResumeError extends Error {
    readonly status: number | null;

    constructor(status: number | null) {
        super(
            status === 410
                ? 'This approval has expired or was already resolved, and can no longer be submitted.'
                : 'Failed to submit your decision. Please try again.'
        );

        this.name = 'ResumeError';
        this.status = status;
    }
}

export interface ApprovalResolutionContextI {
    /**
     * Resolves an approval through the surface's own SSE machinery: the provider POSTs the payload to the
     * SSE-negotiated resume endpoint and pipes the resumed run's output (stream deltas, nested ask-user-question or
     * approval events, errors) back into the conversation through its existing event handlers.
     *
     * <p>The returned promise resolves once the resume request is <b>accepted</b> (2xx and the continuation stream
     * begins) and rejects when it is not (e.g. 410 for an expired/already-resolved
     * approval). Callers should only mark the approval resolved after it settles successfully.
     */
    resolveApproval?: (resumeId: string, payload: Record<string, unknown>) => Promise<void>;
    /**
     * Resolves a gated AI Hub tool call through the {@code resolveAiHubToolApproval} GraphQL mutation. Distinct
     * from {@link resolveApproval}: the tool-approval gate suspends a single tool call (not a whole workflow run)
     * and, on approval, the server starts a fresh agent turn on the same thread rather than resuming a job. Optional
     * because only surfaces that provide this resolution path (AI Hub) render {@link ToolApprovalRequestMessage}
     * interactively; the returned promise settles once the mutation itself completes; the card does not wait for
     * the continuation to finish.
     *
     * <p>Resolves with the row's actual terminal {@code status} (and {@code executionError} when the tool then
     * failed) rather than {@code void}; approving a call that goes on to fail is still a real outcome the card
     * must render honestly, not "Approved" just because the mutation itself returned 2xx. When the approval can no
     * longer be decided it resolves with that status and a {@code resolutionNote} the card shows instead.
     */
    resolveToolApproval?: (
        approvalId: number,
        approved: boolean,
        comment?: string
    ) => Promise<{executionError?: string | null; resolutionNote?: string | null; status: string}>;
}

/**
 * Chat surfaces that can resolve an approval provide this context around their Thread; the inline approval cards use
 * it when present.
 */
export const ApprovalResolutionContext = createContext<ApprovalResolutionContextI | null>(null);

export const useApprovalResolution = () => useContext(ApprovalResolutionContext);
