/* eslint-disable sort-keys */
import {create} from 'zustand';

interface WorkflowExecutionSheetStateI {
    workflowExecutionDetailsSheetOpen: boolean;
    setWorkflowExecutionDetailsSheetOpen: (workflowExecutionDetailsSheetOpen: boolean) => void;

    workflowExecutionId: number;
    setWorkflowExecutionId: (workflowExecutionId: number) => void;
}

export const useWorkflowExecutionSheetStore = create<WorkflowExecutionSheetStateI>()((set) => ({
    workflowExecutionId: 0,
    setWorkflowExecutionId: (workflowExecutionId) =>
        set((state) => ({
            ...state,
            workflowExecutionId: workflowExecutionId,
        })),

    workflowExecutionDetailsSheetOpen: false,
    setWorkflowExecutionDetailsSheetOpen: (workflowExecutionDetailsSheetOpen) =>
        set((state) => ({
            ...state,
            workflowExecutionDetailsSheetOpen: workflowExecutionDetailsSheetOpen,
        })),
}));

export default useWorkflowExecutionSheetStore;
