/* eslint-disable sort-keys */
import {create} from 'zustand';

interface ReadOnlyWorkflowEditorSheetStateI {
    readOnlyWorkflowEditorSheetOpen: boolean;
    setReadOnlyWorkflowEditorSheetOpen: (workflowExecutionSheetOpen: boolean) => void;

    workflowId: string | undefined;
    setWorkflowId: (workflowExecutionId: string | undefined) => void;

    workflowVersion: number | undefined;
    setWorkflowVersion: (setWorkflowVersion: number | undefined) => void;
}

export const useReadOnlyWorkflowEditorSheetStore = create<ReadOnlyWorkflowEditorSheetStateI>()((set) => ({
    workflowId: undefined,
    setWorkflowId: (workflowId) =>
        set((state) => ({
            ...state,
            workflowId,
        })),

    workflowVersion: undefined,
    setWorkflowVersion: (workflowVersion) =>
        set((state) => ({
            ...state,
            workflowVersion,
        })),

    readOnlyWorkflowEditorSheetOpen: false,
    setReadOnlyWorkflowEditorSheetOpen: (readOnlyWorkflowEditorSheetOpen) =>
        set((state) => ({
            ...state,
            readOnlyWorkflowEditorSheetOpen,
        })),
}));

export default useReadOnlyWorkflowEditorSheetStore;
