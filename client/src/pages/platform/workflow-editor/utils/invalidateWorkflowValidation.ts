import {QueryClient} from '@tanstack/react-query';

export const WORKFLOW_VALIDATION_QUERY_KEY = ['ValidateWorkflow'];

export default function invalidateWorkflowValidation(queryClient: QueryClient): void {
    queryClient.invalidateQueries({queryKey: WORKFLOW_VALIDATION_QUERY_KEY});
}
