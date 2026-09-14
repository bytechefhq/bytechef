/* eslint-disable sort-keys */
import {Workflow} from '@/shared/middleware/automation/configuration';
import {create} from 'zustand';

interface OpenProjectDeploymentWorkflowSheetPropsI {
    projectDeploymentId: number;
    projectName?: string;
    projectVersion?: number;
    workflow: Workflow;
}

interface ProjectDeploymentWorkflowSheetStateI {
    projectDeploymentId: number | undefined;
    projectName: string | undefined;
    projectVersion: number | undefined;
    workflow: Workflow | undefined;
    openProjectDeploymentWorkflowSheet: (props: OpenProjectDeploymentWorkflowSheetPropsI) => void;

    projectDeploymentWorkflowSheetOpen: boolean;
    setProjectDeploymentWorkflowSheetOpen: (projectDeploymentWorkflowSheetOpen: boolean) => void;
}

export const useProjectDeploymentWorkflowSheetStore = create<ProjectDeploymentWorkflowSheetStateI>()((set) => ({
    projectDeploymentId: undefined,
    projectName: undefined,
    projectVersion: undefined,
    workflow: undefined,
    openProjectDeploymentWorkflowSheet: ({projectDeploymentId, projectName, projectVersion, workflow}) =>
        set((state) => ({
            ...state,
            projectDeploymentId,
            projectDeploymentWorkflowSheetOpen: true,
            projectName,
            projectVersion,
            workflow,
        })),

    projectDeploymentWorkflowSheetOpen: false,
    setProjectDeploymentWorkflowSheetOpen: (projectDeploymentWorkflowSheetOpen) =>
        set((state) => ({
            ...state,
            projectDeploymentWorkflowSheetOpen,
        })),
}));

export default useProjectDeploymentWorkflowSheetStore;
