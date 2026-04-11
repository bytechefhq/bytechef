import {
    appendToLastAssistantMessage as appendHelper,
    setLastAssistantMessageContent as setContentHelper,
} from '@/shared/util/assistant-message-utils';
import {ThreadMessageLike} from '@assistant-ui/react';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface ConversationCacheEntryI {
    conversationId: string;
    messages: ThreadMessageLike[];
}

interface ChatStateI {
    activeWorkflowExecutionId: string | null;
    conversationCache: Record<string, ConversationCacheEntryI>;
    conversationId: string;
    currentChatName: string | null;
    isRunning: boolean;
    messages: ThreadMessageLike[];
    appendToLastAssistantMessage: (delta: string) => void;
    resetAll: () => void;
    resetCurrentChat: () => void;
    resetMessages: () => void;
    setCurrentChatName: (name: string | null) => void;
    setIsRunning: (isRunning: boolean) => void;
    setLastAssistantMessageContent: (content: string) => void;
    setMessage: (message: ThreadMessageLike) => void;
    switchChat: (workflowExecutionId: string) => void;
}

const generateId = () =>
    Array(32)
        .fill(0)
        .map(() => Math.random().toString(36).charAt(2))
        .join('');

const initialState = {
    activeWorkflowExecutionId: null as string | null,
    conversationCache: {} as Record<string, ConversationCacheEntryI>,
    conversationId: generateId(),
    currentChatName: null as string | null,
    isRunning: false,
    messages: [] as ThreadMessageLike[],
};

export const useChatsStore = create<ChatStateI>()(
    devtools((set) => ({
        ...initialState,
        appendToLastAssistantMessage: (delta: string) =>
            set((state) => ({
                messages: appendHelper(state.messages, delta),
            })),
        resetAll: () => set({...initialState, conversationCache: {}, conversationId: generateId()}),
        resetCurrentChat: () =>
            set((state) => {
                if (state.isRunning) {
                    return state;
                }

                const updatedCache = {...state.conversationCache};

                if (state.activeWorkflowExecutionId) {
                    delete updatedCache[state.activeWorkflowExecutionId];
                }

                return {
                    conversationCache: updatedCache,
                    conversationId: generateId(),
                    messages: [],
                };
            }),
        resetMessages: () => set({messages: []}),
        setCurrentChatName: (name) => set({currentChatName: name}),
        setIsRunning: (isRunning) => set({isRunning}),
        setLastAssistantMessageContent: (content: string) =>
            set((state) => ({
                messages: setContentHelper(state.messages, content),
            })),
        setMessage: (message) =>
            set((state) => ({
                messages: [...state.messages, message],
            })),
        switchChat: (workflowExecutionId: string) =>
            set((state) => {
                if (state.isRunning) {
                    return state;
                }

                const updatedCache = {...state.conversationCache};

                if (state.activeWorkflowExecutionId) {
                    updatedCache[state.activeWorkflowExecutionId] = {
                        conversationId: state.conversationId,
                        messages: state.messages,
                    };
                }

                const cached = updatedCache[workflowExecutionId];

                return {
                    activeWorkflowExecutionId: workflowExecutionId,
                    conversationCache: updatedCache,
                    conversationId: cached?.conversationId ?? generateId(),
                    currentChatName: null,
                    messages: cached?.messages ?? [],
                };
            }),
    }))
);
