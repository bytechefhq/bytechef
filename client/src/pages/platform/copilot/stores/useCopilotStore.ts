import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type ContextType = {
    source: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters: Record<string, any>;
    workflowId: string;
};

interface CopilotStateI {
    conversationId: string | undefined;
    generateConversationId: () => void;

    context: ContextType | undefined;
    setContext: (context: ContextType) => void;

    copilotPanelOpen: boolean;
    setCopilotPanelOpen: (showCopilot: boolean) => void;

    messages: ThreadMessageLike[];
    addMessage: (message: ThreadMessageLike) => void;
    resetMessages: () => void;
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

        context: undefined,
        setContext: (context) =>
            set((state) => {
                return {
                    ...state,
                    context,
                };
            }),

        copilotPanelOpen: false,
        setCopilotPanelOpen: (copilotPanelOpen) =>
            set((state) => {
                return {
                    ...state,
                    copilotPanelOpen,
                };
            }),

        messages: [],
        addMessage: (message) =>
            set((state) => {
                return {
                    ...state,
                    messages: [...state.messages, message],
                };
            }),
        resetMessages: () => set({messages: []}),
    }))
);
