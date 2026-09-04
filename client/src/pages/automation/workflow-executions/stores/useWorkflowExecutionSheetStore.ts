/* eslint-disable sort-keys */
import {WorkflowExecutionKindType} from '@/shared/queries/automation/workflowExecutions.queries';
import {create} from 'zustand';

interface WorkflowExecutionSheetStateI {
    workflowExecutionSheetOpen: boolean;
    setWorkflowExecutionSheetOpen: (workflowExecutionDetailsSheetOpen: boolean) => void;

    workflowExecutionId: number;
    workflowExecutionKind: WorkflowExecutionKindType;
    setWorkflowExecutionId: (workflowExecutionId: number, workflowExecutionKind?: WorkflowExecutionKindType) => void;
}

export const useWorkflowExecutionSheetStore = create<WorkflowExecutionSheetStateI>()((set) => ({
    workflowExecutionId: 0,
    workflowExecutionKind: 'JOB',
    setWorkflowExecutionId: (workflowExecutionId, workflowExecutionKind = 'JOB') =>
        set((state) => ({
            ...state,
            workflowExecutionId: workflowExecutionId,
            workflowExecutionKind: workflowExecutionKind,
        })),

    workflowExecutionSheetOpen: false,
    setWorkflowExecutionSheetOpen: (workflowExecutionSheetOpen) =>
        set((state) => ({
            ...state,
            workflowExecutionSheetOpen: workflowExecutionSheetOpen,
        })),
}));

export default useWorkflowExecutionSheetStore;
