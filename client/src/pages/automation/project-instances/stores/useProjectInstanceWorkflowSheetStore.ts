/* eslint-disable sort-keys */
import {create} from 'zustand';

interface ProjectInstanceWorkflowSheetStateI {
    projectInstanceWorkflowSheetOpen: boolean;
    setProjectInstanceWorkflowSheetOpen: (workflowExecutionSheetOpen: boolean) => void;

    workflowId: string | undefined;
    setWorkflowId: (workflowExecutionId: string | undefined) => void;

    workflowVersion: number | undefined;
    setWorkflowVersion: (setWorkflowVersion: number | undefined) => void;
}

export const useProjectInstanceWorkflowSheetStore = create<ProjectInstanceWorkflowSheetStateI>()((set) => ({
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

    projectInstanceWorkflowSheetOpen: false,
    setProjectInstanceWorkflowSheetOpen: (projectInstanceWorkflowSheetOpen) =>
        set((state) => ({
            ...state,
            projectInstanceWorkflowSheetOpen,
        })),
}));

export default useProjectInstanceWorkflowSheetStore;
