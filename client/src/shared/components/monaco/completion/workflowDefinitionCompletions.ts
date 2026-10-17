import type {WorkflowDefinitionCompletionContextI} from './completionTypes';
import type {CompletionItemDraftType} from './scriptCompletions';

export interface WorkflowDefinitionCompletionRequestI {
    context: WorkflowDefinitionCompletionContextI;
    cursorOffset: number;
    source: string;
}

export const getWorkflowDefinitionCompletions: (
    request: WorkflowDefinitionCompletionRequestI
) => Promise<CompletionItemDraftType[]> = () => Promise.resolve([]);
