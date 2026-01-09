import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowChatStateI {
    conversationId: string;
    messages: ThreadMessageLike[];
    setMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (delta: string) => void;
    setLastAssistantMessageContent: (content: string) => void;
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
};

export const useWorkflowChatStore = create<WorkflowChatStateI>()(
    devtools((set) => ({
        ...initialState,
        setMessage: (message) =>
            set((state) => ({
                messages: [...state.messages, message],
            })),
        appendToLastAssistantMessage: (delta: string) =>
            set((state) => {
                const messages = [...state.messages];
                // find last assistant message
                for (let i = messages.length - 1; i >= 0; i--) {
                    const msg = messages[i] as ThreadMessageLike & {content?: string; role?: string};
                    if (msg && msg.role === 'assistant') {
                        const current = typeof msg.content === 'string' ? msg.content : '';
                        const chunk = typeof delta === 'string' ? delta : String(delta ?? '');
                        messages[i] = {...msg, content: current + chunk};
                        return {messages};
                    }
                }
                // no assistant message yet; create one
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
        resetMessages: () => set({messages: []}),
        reset: () => set({...initialState, conversationId: generateId()}),
    }))
);
