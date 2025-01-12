import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowChatStateI {
    conversationId: string | undefined;
    generateConversationId: () => void;
    messages: ThreadMessageLike[];
    setMessage: (message: ThreadMessageLike) => void;
    resetMessages: () => void;
}

export const useWorkflowChatStore = create<WorkflowChatStateI>()(
    devtools((set) => ({
        conversationId: undefined,
        generateConversationId: () => {
            set((state) => {
                return {
                    ...state,
                    conversationId: Array(32)
                        .fill(0)
                        .map(() => Math.random().toString(36).charAt(2))
                        .join(''),
                };
            });
        },

        messages: [],
        setMessage: (message) =>
            set((state) => {
                return {
                    ...state,
                    messages: [...state.messages, message],
                };
            }),
        resetMessages: () => set({messages: []}),
    }))
);
