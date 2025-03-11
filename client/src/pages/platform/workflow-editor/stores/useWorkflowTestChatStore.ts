import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowTestChatStateI {
    conversationId: string | undefined;
    generateConversationId: () => void;

    messages: ThreadMessageLike[];
    setMessage: (message: ThreadMessageLike) => void;
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
