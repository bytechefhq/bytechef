import {createContext, useContext} from 'react';
import type {PendingApprovalI} from '@/stores/useChatStore';

export interface ApprovalResolutionContextI {
    resolveApproval: (pendingApproval: PendingApprovalI, payload: Record<string, unknown>) => Promise<void>;
}

export const ApprovalResolutionContext = createContext<ApprovalResolutionContextI | null>(null);

export const useApprovalResolution = () => useContext(ApprovalResolutionContext);
