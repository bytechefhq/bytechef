/* eslint-disable sort-keys */
import {create} from 'zustand';

export interface AutomationExecutionsSheetWorkflowI {
    label?: string | null;
    workflowId: string;
    workflowUuid: string;
}

interface OpenAutomationExecutionsSheetPropsI {
    connectedUserId?: number;
    environmentId: number;
    projectName?: string;
    workflow: AutomationExecutionsSheetWorkflowI;
}

interface AutomationExecutionsSheetStateI {
    connectedUserId: number | undefined;
    environmentId: number | undefined;
    projectName: string | undefined;
    workflow: AutomationExecutionsSheetWorkflowI | undefined;
    openAutomationExecutionsSheet: (props: OpenAutomationExecutionsSheetPropsI) => void;

    automationExecutionsSheetOpen: boolean;
    setAutomationExecutionsSheetOpen: (automationExecutionsSheetOpen: boolean) => void;
}

export const useAutomationExecutionsSheetStore = create<AutomationExecutionsSheetStateI>()((set) => ({
    connectedUserId: undefined,
    environmentId: undefined,
    projectName: undefined,
    workflow: undefined,
    openAutomationExecutionsSheet: ({connectedUserId, environmentId, projectName, workflow}) =>
        set((state) => ({
            ...state,
            automationExecutionsSheetOpen: true,
            connectedUserId,
            environmentId,
            projectName,
            workflow,
        })),

    automationExecutionsSheetOpen: false,
    setAutomationExecutionsSheetOpen: (automationExecutionsSheetOpen) =>
        set((state) => ({
            ...state,
            automationExecutionsSheetOpen,
        })),
}));

export default useAutomationExecutionsSheetStore;
