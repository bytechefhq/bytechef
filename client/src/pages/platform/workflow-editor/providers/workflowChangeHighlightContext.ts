import {WorkflowNodeChangeKind} from '@/shared/middleware/graphql';
import {createContext, useContext} from 'react';

export const WorkflowChangeHighlightContext = createContext<Record<string, WorkflowNodeChangeKind> | null>(null);

export function useWorkflowChangeHighlight(): Record<string, WorkflowNodeChangeKind> | null {
    return useContext(WorkflowChangeHighlightContext);
}
