import {ThreadMessageLike} from '@assistant-ui/react';
import {create} from 'zustand';

interface ChatState {
    conversationId: string;
    messages: ThreadMessageLike[];
    resumeUrl: string | null;
    setMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (delta: string) => void;
    setLastAssistantMessageContent: (content: string) => void;
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
    setResumeUrl: (resumeUrl) => set({resumeUrl}),
    resetMessages: () => set({messages: [], resumeUrl: null}),
    reset: () => set({...initialState, conversationId: generateId()}),
}));
