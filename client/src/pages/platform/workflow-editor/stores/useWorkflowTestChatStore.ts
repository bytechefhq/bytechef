import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowTestChatStateI {
    conversationId: string | undefined;
    generateConversationId: () => void;

    messages: ThreadMessageLike[];
    setMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (delta: string) => void;
    setLastAssistantMessageContent: (content: string) => void;
    resetMessages: () => void;

    workflowTestChatPanelOpen: boolean;
    setWorkflowTestChatPanelOpen: (workflowTestChatPanelOpen: boolean) => void;
}

const useWorkflowTestChatStore = create<WorkflowTestChatStateI>()(
    devtools(
        (set) => ({
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
                            return {...state, messages};
                        }
                    }
                    // no assistant message yet; create one
                    return {
                        ...state,
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
                            return {...state, messages};
                        }
                    }
                    return {
                        ...state,
                        messages: [...messages, {content, role: 'assistant'} as ThreadMessageLike],
                    };
                }),
            resetMessages: () => set({messages: []}),

            workflowTestChatPanelOpen: false,
            setWorkflowTestChatPanelOpen: (workflowTestChatPanelOpen) =>
                set((state) => ({
                    ...state,
                    workflowTestChatPanelOpen,
                })),
        }),
        {
            name: 'workflow-node-details-panel',
        }
    )
);

export default useWorkflowTestChatStore;
