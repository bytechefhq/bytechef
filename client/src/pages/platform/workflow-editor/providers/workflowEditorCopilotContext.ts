import {createContext, useContext} from 'react';

export const WorkflowEditorCopilotContext = createContext<boolean>(true);

export function useWorkflowEditorCopilotAllowed(): boolean {
    return useContext(WorkflowEditorCopilotContext);
}
