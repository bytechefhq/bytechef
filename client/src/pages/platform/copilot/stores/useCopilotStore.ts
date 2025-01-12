import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CopilotStateI {
    conversationId: string | undefined;
    generateConversationId: () => void;
    copilotPanelOpen: boolean;
    messages: ThreadMessageLike[];
    setMessage: (message: ThreadMessageLike) => void;
    resetMessages: () => void;
    setCopilotPanelOpen: (showCopilot: boolean) => void;
}

export const useCopilotStore = create<CopilotStateI>()(
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

        copilotPanelOpen: false,
        setCopilotPanelOpen: (copilotPanelOpen) =>
            set((state) => {
                return {
                    ...state,
                    copilotPanelOpen,
                };
            }),

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
