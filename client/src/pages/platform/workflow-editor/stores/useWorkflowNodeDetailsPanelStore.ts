/* eslint-disable sort-keys */
import {ComponentType, NodeDataType} from '@/shared/types';
import {Editor} from '@tiptap/react';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowNodeDetailsPanelStoreI {
    activeTab: string;
    setActiveTab: (activeTab: string) => void;

    connectionDialogAllowed: boolean;
    setConnectionDialogAllowed: (connectionDialogAllowed: boolean) => void;

    currentComponent: ComponentType | undefined;
    setCurrentComponent: (
        currentComponent:
            | ComponentType
            | undefined
            | ((previousCurrentComponent: ComponentType | undefined) => ComponentType | undefined)
    ) => void;

    currentNode: NodeDataType | undefined;
    setCurrentNode: (currentNode: NodeDataType | undefined) => void;

    focusedInput: Editor | null;
    setFocusedInput: (focusedInput: Editor | null) => void;

    reset: () => void;

    workflowNodeDetailsPanelOpen: boolean;
    setWorkflowNodeDetailsPanelOpen: (workflowNodeDetailsPanelOpen: boolean) => void;
}

const useWorkflowNodeDetailsPanelStore = create<WorkflowNodeDetailsPanelStoreI>()(
    devtools(
        (set) => ({
            activeTab: 'description',
            setActiveTab: (activeTab) => set((state) => ({...state, activeTab})),

            connectionDialogAllowed: true,
            setConnectionDialogAllowed: (connectionDialogAllowed) =>
                set((state) => ({...state, connectionDialogAllowed})),

            currentComponent: undefined,
            setCurrentComponent: (currentComponent) =>
                set((state) => ({
                    ...state,
                    currentComponent:
                        typeof currentComponent === 'function'
                            ? currentComponent(state.currentComponent)
                            : currentComponent,
                })),

            currentNode: undefined,
            setCurrentNode: (currentNode) => set((state) => ({...state, currentNode})),

            focusedInput: null,
            setFocusedInput: (focusedInput) => set((state) => ({...state, focusedInput})),

            reset: () =>
                set(() => ({
                    currentComponent: undefined,
                    currentComponentDefinition: undefined,
                    currentNode: undefined,
                    focusedInput: null,
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
