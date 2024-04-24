import {ComponentDefinitionModel} from '@/middleware/platform/configuration';

/* eslint-disable sort-keys */
import {ComponentType, NodeType} from '@/types/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowNodeDetailsPanelStateI {
    currentComponent: ComponentType | undefined;
    setCurrentComponent: (currentComponent: ComponentType | undefined) => void;

    currentComponentDefinition: ComponentDefinitionModel | undefined;
    setCurrentComponentDefinition: (currentComponentDefinition: ComponentDefinitionModel | undefined) => void;

    currentNode: NodeType | undefined;
    setCurrentNode: (currentNode: NodeType | undefined) => void;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    focusedInput: any;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    setFocusedInput: (focusedInput: any) => void;

    workflowNodeDetailsPanelOpen: boolean;
    setWorkflowNodeDetailsPanelOpen: (workflowNodeDetailsPanelOpen: boolean) => void;
}

export const useWorkflowNodeDetailsPanelStore = create<WorkflowNodeDetailsPanelStateI>()(
    devtools(
        (set) => ({
            currentComponent: undefined,
            setCurrentComponent: (currentComponent) => set((state) => ({...state, currentComponent})),

            currentComponentDefinition: undefined,
            setCurrentComponentDefinition: (currentComponentDefinition) =>
                set((state) => ({...state, currentComponentDefinition})),

            currentNode: undefined,
            setCurrentNode: (currentNode) => set((state) => ({...state, currentNode})),

            focusedInput: null,
            setFocusedInput: (focusedInput) => set((state) => ({...state, focusedInput})),

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
