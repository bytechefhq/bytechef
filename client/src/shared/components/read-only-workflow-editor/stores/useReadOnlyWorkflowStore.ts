/* eslint-disable sort-keys */
import {Workflow} from '@/shared/middleware/platform/configuration';
import {create} from 'zustand';

interface ReadOnlyWorkflowStoreI {
    isReadOnlyWorkflowSheetOpen: boolean;
    setIsReadOnlyWorkflowSheetOpen: (workflowExecutionSheetOpen: boolean) => void;

    workflow: Workflow | undefined;
    setWorkflow: (workflow: Workflow | undefined) => void;
}

export const useReadOnlyWorkflowStore = create<ReadOnlyWorkflowStoreI>()((set) => ({
    workflow: undefined,
    setWorkflow: (workflow) =>
        set((state) => ({
            ...state,
            workflow,
        })),

    isReadOnlyWorkflowSheetOpen: false,
    setIsReadOnlyWorkflowSheetOpen: (readOnlyWorkflowSheetOpen) =>
        set((state) => ({
            ...state,
            isReadOnlyWorkflowSheetOpen: readOnlyWorkflowSheetOpen,
        })),
}));

export default useReadOnlyWorkflowStore;
