import {ThreadMessageLike} from '@assistant-ui/react';
import {create} from 'zustand';

interface ChatState {
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

export const useChatStore = create<ChatState>((set) => ({
    ...initialState,
    setMessage: (message) =>
        set((state) => {
            console.log('[useChatStore] setMessage called with:', message);
            const newMessages = [...state.messages, message];
            console.log('[useChatStore] Updated messages:', newMessages);
            return {messages: newMessages};
        }),
    appendToLastAssistantMessage: (delta: string) =>
        set((state) => {
            console.log('[useChatStore] appendToLastAssistantMessage called with delta:', delta);
            const messages = [...state.messages];
            // find last assistant message
            for (let i = messages.length - 1; i >= 0; i--) {
                const msg = messages[i] as ThreadMessageLike & {content?: string; role?: string};

                if (msg && msg.role === 'assistant') {
                    const current = typeof msg.content === 'string' ? msg.content : '';
                    const chunk = typeof delta === 'string' ? delta : String(delta ?? '');
                    messages[i] = {...msg, content: current + chunk};

                    console.log('[useChatStore] Updated assistant message:', messages[i]);
                    return {messages};
                }
            }
            // no assistant message yet; create one
            console.log('[useChatStore] Creating new assistant message');
            return {
                messages: [...messages, {content: delta, role: 'assistant'} as ThreadMessageLike],
            };
        }),
    setLastAssistantMessageContent: (content: string) =>
        set((state) => {
            console.log('[useChatStore] setLastAssistantMessageContent called with:', content);
            const messages = [...state.messages];

            for (let i = messages.length - 1; i >= 0; i--) {
                const msg = messages[i] as ThreadMessageLike & {content?: string; role?: string};

                if (msg && msg.role === 'assistant') {
                    messages[i] = {...msg, content};

                    console.log('[useChatStore] Updated last assistant message content:', messages[i]);
                    return {messages};
                }
            }

            console.log('[useChatStore] Creating new assistant message with content');
            return {
                messages: [...messages, {content, role: 'assistant'} as ThreadMessageLike],
            };
        }),
    resetMessages: () => set({messages: []}),
    reset: () => set({...initialState, conversationId: generateId()}),
}));
