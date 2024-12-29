/* eslint-disable sort-keys */
import {ComponentType, NodeDataType} from '@/shared/types';
import {Editor} from '@tiptap/react';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowNodeDetailsPanelStoreI {
    currentComponent: ComponentType | undefined;
    setCurrentComponent: (currentComponent: ComponentType | undefined) => void;

    currentNode: NodeDataType | undefined;
    setCurrentNode: (currentNode: NodeDataType | undefined) => void;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    focusedInput: Editor | null;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    setFocusedInput: (focusedInput: Editor | null) => void;

    reset: () => void;

    workflowNodeDetailsPanelOpen: boolean;
    setWorkflowNodeDetailsPanelOpen: (workflowNodeDetailsPanelOpen: boolean) => void;
}

const useWorkflowNodeDetailsPanelStore = create<WorkflowNodeDetailsPanelStoreI>()(
    devtools(
        (set) => ({
            currentComponent: undefined,
            setCurrentComponent: (currentComponent) => set((state) => ({...state, currentComponent})),

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
