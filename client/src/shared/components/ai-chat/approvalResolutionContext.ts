import {createContext, useContext} from 'react';

/**
 * Thrown when the resume request itself is rejected (non-2xx or a connection failure) — carries the HTTP status so
 * callers can special-case 410 (expired / already resolved). Its {@code message} is already user-facing.
 */
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
     * begins) and rejects with a {@link ResumeError} when it is not (e.g. 410 for an expired/already-resolved
     * approval). Callers should only mark the approval resolved after it settles successfully.
     */
    resolveApproval: (resumeId: string, payload: Record<string, unknown>) => Promise<void>;
}

/**
 * Chat surfaces that can stream an approval resolution's continuation provide this context around their Thread; the
 * inline approval card (ApprovalRequestMessage) uses it when present and falls back to the plain (non-streaming)
 * resume mutation when absent.
 */
export const ApprovalResolutionContext = createContext<ApprovalResolutionContextI | null>(null);

export const useApprovalResolution = () => useContext(ApprovalResolutionContext);
