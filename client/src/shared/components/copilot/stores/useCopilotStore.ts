import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export enum MODE {
    CHAT = 'CHAT',
    BUILD = 'BUILD',
}

export enum Source {
    WORKFLOW_EDITOR,
    WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU,
    CODE_EDITOR,
}

export type ContextType = {
    source: Source;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters: Record<string, any>;
    mode: MODE;
};

interface CopilotStateI {
    conversationId: string | undefined;
    generateConversationId: () => void;

    context: ContextType;
    setContext: (context: ContextType | undefined) => void;

    copilotPanelOpen: boolean;
    setCopilotPanelOpen: (showCopilot: boolean) => void;

    messages: ThreadMessageLike[];
    addMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (text: string) => void;
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

        context: {
            mode: MODE.CHAT,
        },
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
        appendToLastAssistantMessage: (text: string) =>
            set((state) => {
                const messages = [...state.messages];

                // find last assistant message
                for (let i = messages.length - 1; i >= 0; i--) {
                    const message = messages[i] as ThreadMessageLike;

                    if (message.role === 'assistant' && typeof message.content === 'string') {
                        messages[i] = {...message, content: message.content + text} as ThreadMessageLike;

                        return {...state, messages};
                    }
                }

                // no assistant message yet; create one
                messages.push({role: 'assistant', content: text} as ThreadMessageLike);

                return {...state, messages};
            }),
        resetMessages: () => set({messages: []}),
    }))
);
