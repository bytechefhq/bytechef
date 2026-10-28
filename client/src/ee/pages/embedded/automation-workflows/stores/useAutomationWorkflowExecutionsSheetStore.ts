/* eslint-disable sort-keys */
import {create} from 'zustand';

export interface AutomationWorkflowExecutionsSheetWorkflowI {
    label?: string | null;
    workflowId: string;
    workflowUuid: string;
}

interface OpenAutomationWorkflowExecutionsSheetPropsI {
    connectedUserId?: number;
    projectName?: string;
    workflow: AutomationWorkflowExecutionsSheetWorkflowI;
}

interface AutomationWorkflowExecutionsSheetStateI {
    connectedUserId: number | undefined;
    projectName: string | undefined;
    workflow: AutomationWorkflowExecutionsSheetWorkflowI | undefined;
    openAutomationWorkflowExecutionsSheet: (props: OpenAutomationWorkflowExecutionsSheetPropsI) => void;

    automationWorkflowExecutionsSheetOpen: boolean;
    setAutomationWorkflowExecutionsSheetOpen: (automationWorkflowExecutionsSheetOpen: boolean) => void;
}

export const useAutomationWorkflowExecutionsSheetStore = create<AutomationWorkflowExecutionsSheetStateI>()((set) => ({
    connectedUserId: undefined,
    projectName: undefined,
    workflow: undefined,
    openAutomationWorkflowExecutionsSheet: ({connectedUserId, projectName, workflow}) =>
        set((state) => ({
            ...state,
            automationWorkflowExecutionsSheetOpen: true,
            connectedUserId,
            projectName,
            workflow,
        })),

    automationWorkflowExecutionsSheetOpen: false,
    setAutomationWorkflowExecutionsSheetOpen: (automationWorkflowExecutionsSheetOpen) =>
        set((state) => ({
            ...state,
            automationWorkflowExecutionsSheetOpen,
        })),
}));

export default useAutomationWorkflowExecutionsSheetStore;
