/* eslint-disable sort-keys */
import {create} from 'zustand';

interface ProjectDeploymentWorkflowSheetStateI {
    projectDeploymentWorkflowSheetOpen: boolean;
    setProjectDeploymentWorkflowSheetOpen: (workflowExecutionSheetOpen: boolean) => void;

    workflowId: string | undefined;
    setWorkflowId: (workflowExecutionId: string | undefined) => void;

    workflowVersion: number | undefined;
    setWorkflowVersion: (setWorkflowVersion: number | undefined) => void;
}

export const useProjectDeploymentWorkflowSheetStore = create<ProjectDeploymentWorkflowSheetStateI>()((set) => ({
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

    projectDeploymentWorkflowSheetOpen: false,
    setProjectDeploymentWorkflowSheetOpen: (projectDeploymentWorkflowSheetOpen) =>
        set((state) => ({
            ...state,
            projectDeploymentWorkflowSheetOpen: projectDeploymentWorkflowSheetOpen,
        })),
}));

export default useProjectDeploymentWorkflowSheetStore;
