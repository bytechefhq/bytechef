/* eslint-disable sort-keys */
import {create} from 'zustand';

interface WorkflowExecutionDetailsStateI {
    workflowExecutionDetailsDialogOpen: boolean;
    setWorkflowExecutionDetailsDialogOpen: (workflowExecutionDetailsDialogOpen: boolean) => void;

    workflowExecutionId: number;
    setWorkflowExecutionId: (workflowExecutionId: number) => void;
}

export const useWorkflowExecutionDetailsDialogStore = create<WorkflowExecutionDetailsStateI>()((set) => ({
    workflowExecutionId: 0,
    setWorkflowExecutionId: (workflowExecutionId) =>
        set((state) => ({
            ...state,
            workflowExecutionId: workflowExecutionId,
        })),

    workflowExecutionDetailsDialogOpen: false,
    setWorkflowExecutionDetailsDialogOpen: (executionDetailsDialogOpen) =>
        set((state) => ({
            ...state,
            workflowExecutionDetailsDialogOpen: executionDetailsDialogOpen,
        })),
}));

export default useWorkflowExecutionDetailsDialogStore;
