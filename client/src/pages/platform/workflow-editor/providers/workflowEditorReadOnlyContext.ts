import {createContext, useContext} from 'react';

export const WorkflowEditorReadOnlyContext = createContext<boolean>(false);

export function useWorkflowEditorReadOnly(): boolean {
    return useContext(WorkflowEditorReadOnlyContext);
}
