/* eslint-disable sort-keys */
import {NodeDataType} from '@/shared/types';
import {Editor} from '@tiptap/react';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowNodeDetailsPanelStoreI {
    activeTab: string;
    setActiveTab: (activeTab: string) => void;

    aiAgentNodeDetailsPanelOpen: boolean;
    setAiAgentNodeDetailsPanelOpen: (aiAgentNodeDetailsPanelOpen: boolean) => void;

    connectionDialogAllowed: boolean;
    setConnectionDialogAllowed: (connectionDialogAllowed: boolean) => void;

    currentNode: NodeDataType | undefined;
    setCurrentNode: (
        currentNode:
            | NodeDataType
            | undefined
            | ((previousCurrentNode: NodeDataType | undefined) => NodeDataType | undefined)
    ) => void;

    focusedInput: Editor | null;
    setFocusedInput: (focusedInput: Editor | null) => void;

    operationChangeInProgress: boolean;
    setOperationChangeInProgress: (operationChangeInProgress: boolean) => void;

    pendingSaveNodeNames: ReadonlySet<string>;
    addPendingSaveNodeName: (nodeName: string) => void;
    clearPendingSaveNodeNames: () => void;
    removePendingSaveNodeName: (nodeName: string) => void;

    reset: () => void;

    workflowNodeDetailsPanelOpen: boolean;
    setWorkflowNodeDetailsPanelOpen: (workflowNodeDetailsPanelOpen: boolean) => void;
}

const useWorkflowNodeDetailsPanelStore = create<WorkflowNodeDetailsPanelStoreI>()(
    devtools(
        (set) => ({
            activeTab: 'description',
            setActiveTab: (activeTab) => set((state) => ({...state, activeTab})),

            aiAgentNodeDetailsPanelOpen: false,
            setAiAgentNodeDetailsPanelOpen: (aiAgentNodeDetailsPanelOpen) =>
                set((state) => ({...state, aiAgentNodeDetailsPanelOpen})),

            connectionDialogAllowed: true,
            setConnectionDialogAllowed: (connectionDialogAllowed) =>
                set((state) => ({...state, connectionDialogAllowed})),

            currentNode: undefined,
            setCurrentNode: (currentNode) =>
                set((state) => ({
                    ...state,
                    currentNode: typeof currentNode === 'function' ? currentNode(state.currentNode) : currentNode,
                })),

            focusedInput: null,
            setFocusedInput: (focusedInput) => set((state) => ({...state, focusedInput})),

            operationChangeInProgress: false,
            setOperationChangeInProgress: (operationChangeInProgress) =>
                set((state) => ({...state, operationChangeInProgress})),

            pendingSaveNodeNames: new Set<string>(),
            addPendingSaveNodeName: (nodeName) =>
                set((state) => ({...state, pendingSaveNodeNames: new Set([...state.pendingSaveNodeNames, nodeName])})),
            clearPendingSaveNodeNames: () => set((state) => ({...state, pendingSaveNodeNames: new Set<string>()})),
            removePendingSaveNodeName: (nodeName) =>
                set((state) => {
                    if (!state.pendingSaveNodeNames.has(nodeName)) {
                        return state;
                    }

                    const pendingSaveNodeNames = new Set(state.pendingSaveNodeNames);

                    pendingSaveNodeNames.delete(nodeName);

                    return {...state, pendingSaveNodeNames};
                }),

            reset: () =>
                set(() => ({
                    aiAgentNodeDetailsPanelOpen: false,
                    currentNode: undefined,
                    focusedInput: null,
                    operationChangeInProgress: false,
                    workflowNodeDetailsPanelOpen: false,
                })),

            workflowNodeDetailsPanelOpen: false,
            setWorkflowNodeDetailsPanelOpen: (workflowNodeDetailsPanelOpen) =>
                set((state) => ({
                    ...state,
                    workflowNodeDetailsPanelOpen: workflowNodeDetailsPanelOpen,
                })),
        }),
        {
            name: 'workflow-node-details-panel',
        }
    )
);

export default useWorkflowNodeDetailsPanelStore;
