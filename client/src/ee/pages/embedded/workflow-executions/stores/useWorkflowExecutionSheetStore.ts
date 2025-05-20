/* eslint-disable sort-keys */
import {create} from 'zustand';

interface WorkflowExecutionSheetStateI {
    workflowExecutionSheetOpen: boolean;
    setWorkflowExecutionSheetOpen: (workflowExecutionSheetOpen: boolean) => void;

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

    workflowExecutionSheetOpen: false,
    setWorkflowExecutionSheetOpen: (workflowExecutionSheetOpen) =>
        set((state) => ({
            ...state,
            workflowExecutionSheetOpen: workflowExecutionSheetOpen,
        })),
}));

export default useWorkflowExecutionSheetStore;
