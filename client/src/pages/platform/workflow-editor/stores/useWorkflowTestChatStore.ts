import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowTestChatStateI {
    messages: ThreadMessageLike[];
    setMessage: (message: ThreadMessageLike) => void;
    resetMessages: () => void;
    workflowTestChatPanelOpen: boolean;
    setWorkflowTestChatPanelOpen: (workflowTestChatPanelOpen: boolean) => void;
}

const useWorkflowTestChatStore = create<WorkflowTestChatStateI>()(
    devtools(
        (set) => ({
            messages: [],
            setMessage: (message) =>
                set((state) => {
                    return {
                        ...state,
                        messages: [...state.messages, message],
                    };
                }),
            resetMessages: () => set({messages: []}),

            testWorkflowChatPanelOpen: false,
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
