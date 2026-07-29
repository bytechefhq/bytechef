import {createContext, useContext} from 'react';
import type {PendingApprovalI} from '@/stores/useChatStore';

export interface ApprovalResolutionContextI {
    /**
     * Resolves a pending approval through the SSE-negotiated resume endpoint and streams the resumed run's output
     * back into the widget conversation. Returns once the resume request is accepted (the continuation keeps
     * streaming in the background); throws when the resume request fails so the card can surface the error.
     */
    resolveApproval: (pendingApproval: PendingApprovalI, payload: Record<string, unknown>) => Promise<void>;
}

/**
 * Provided by AutomationChatProvider so the inline ApprovalCard resolves with continuation streaming; the card
 * falls back to a plain resume POST when no provider is present (e.g. isolated component tests).
 */
export const ApprovalResolutionContext = createContext<ApprovalResolutionContextI | null>(null);

export const useApprovalResolution = () => useContext(ApprovalResolutionContext);
