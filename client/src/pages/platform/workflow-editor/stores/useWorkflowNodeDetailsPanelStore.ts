/* eslint-disable sort-keys */
import {DataPillType, NodeType} from '@/types/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface WorkflowNodeDetailsPanelStateI {
    copiedPropertyData: DataPillType;
    setCopiedPropertyData: (copiedPropertyData: DataPillType) => void;

    currentNode: NodeType;
    setCurrentNode: (currentNode: NodeType) => void;

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
            copiedPropertyData: {} as DataPillType,
            setCopiedPropertyData: (copiedPropertyData) => set((state) => ({...state, copiedPropertyData})),

            currentNode: {id: '', name: '', type: 'component', version: 1},
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
