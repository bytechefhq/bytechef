import {
    appendToLastAssistantMessage as appendHelper,
    setLastAssistantMessageContent as setContentHelper,
} from '@/shared/util/assistant-message-utils';
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
            set((state) => ({
                messages: appendHelper(state.messages, delta),
            })),
        setLastAssistantMessageContent: (content: string) =>
            set((state) => ({
                messages: setContentHelper(state.messages, content),
            })),
        resetMessages: () => set({messages: []}),
        reset: () => set({...initialState, conversationId: generateId()}),
    }))
);
