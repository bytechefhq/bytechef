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
    resolveApproval: (resumeId: string, payload: Record<string, unknown>) => Promise<void>;
}

export const ApprovalResolutionContext = createContext<ApprovalResolutionContextI | null>(null);

export const useApprovalResolution = () => useContext(ApprovalResolutionContext);
