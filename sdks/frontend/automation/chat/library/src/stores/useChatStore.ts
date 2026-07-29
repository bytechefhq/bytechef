import {ThreadMessageLike} from '@assistant-ui/react';
import {create} from 'zustand';

/**
 * A field-less approval request awaiting an inline decision. Rendered by ApprovalCard above the composer; resolved
 * by POSTing {approved, comment?} to resumeUrl. Approvals with form fields are not held here — they render as a
 * markdown link to the hosted approval form instead.
 */
export interface PendingApprovalI {
    expiresAt?: string;
    formDescription?: string;
    formTitle?: string;
    resumeUrl: string;
}

interface ChatState {
    conversationId: string;
    messages: ThreadMessageLike[];
    pendingApproval: PendingApprovalI | null;
    resumeUrl: string | null;
    setMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (delta: string) => void;
    setLastAssistantMessageContent: (content: string) => void;
    setPendingApproval: (pendingApproval: PendingApprovalI | null) => void;
    setResumeUrl: (resumeUrl: string | null) => void;
    resetMessages: () => void;
    reset: () => void;
}

const generateId = () =>
    Array(32)
        .fill(0)
        .map(() => Math.random().toString(36).charAt(2))
        .join('');

const initialState = {
    conversationId: generateId(),
    messages: [] as ThreadMessageLike[],
    pendingApproval: null as PendingApprovalI | null,
    resumeUrl: null as string | null,
};

export const useChatStore = create<ChatState>((set) => ({
    ...initialState,
    setMessage: (message) =>
        set((state) => {
            const newMessages = [...state.messages, message];

            return {messages: newMessages};
        }),
    appendToLastAssistantMessage: (delta: string) =>
        set((state) => {
            const messages = [...state.messages];

            for (let i = messages.length - 1; i >= 0; i--) {
                const msg = messages[i] as ThreadMessageLike & {content?: string; role?: string};

                if (msg && msg.role === 'assistant') {
                    const current = typeof msg.content === 'string' ? msg.content : '';
                    const chunk = typeof delta === 'string' ? delta : String(delta ?? '');

                    messages[i] = {...msg, content: current + chunk};

                    return {messages};
                }
            }

            return {
                messages: [...messages, {content: delta, role: 'assistant'} as ThreadMessageLike],
            };
        }),
    setLastAssistantMessageContent: (content: string) =>
        set((state) => {
            const messages = [...state.messages];

            for (let i = messages.length - 1; i >= 0; i--) {
                const msg = messages[i] as ThreadMessageLike & {content?: string; role?: string};

                if (msg && msg.role === 'assistant') {
                    messages[i] = {...msg, content};

                    return {messages};
                }
            }

            return {
                messages: [...messages, {content, role: 'assistant'} as ThreadMessageLike],
            };
        }),
    setPendingApproval: (pendingApproval) => set({pendingApproval}),
    setResumeUrl: (resumeUrl) => set({resumeUrl}),
    resetMessages: () => set({messages: [], pendingApproval: null, resumeUrl: null}),
    reset: () => set({...initialState, conversationId: generateId()}),
}));
