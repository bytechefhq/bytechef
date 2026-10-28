/* eslint-disable sort-keys */
import {create} from 'zustand';

export interface AutomationWorkflowExecutionsSheetWorkflowI {
    label?: string | null;
    workflowId: string;
    workflowUuid: string;
}

interface OpenAutomationWorkflowExecutionsSheetPropsI {
    projectName?: string;
    workflow: AutomationWorkflowExecutionsSheetWorkflowI;
}

interface AutomationWorkflowExecutionsSheetStateI {
    projectName: string | undefined;
    workflow: AutomationWorkflowExecutionsSheetWorkflowI | undefined;
    openAutomationWorkflowExecutionsSheet: (props: OpenAutomationWorkflowExecutionsSheetPropsI) => void;

    automationWorkflowExecutionsSheetOpen: boolean;
    setAutomationWorkflowExecutionsSheetOpen: (automationWorkflowExecutionsSheetOpen: boolean) => void;
}

export const useAutomationWorkflowExecutionsSheetStore = create<AutomationWorkflowExecutionsSheetStateI>()((set) => ({
    projectName: undefined,
    workflow: undefined,
    openAutomationWorkflowExecutionsSheet: ({projectName, workflow}) =>
        set((state) => ({
            ...state,
            automationWorkflowExecutionsSheetOpen: true,
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
