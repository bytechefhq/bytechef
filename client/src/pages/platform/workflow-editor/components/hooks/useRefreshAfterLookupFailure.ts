import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';

import {WorkflowIssueI} from '../../stores/useWorkflowIssuesStore';
import invalidateWorkflowValidation from '../../utils/invalidateWorkflowValidation';

interface UseRefreshAfterLookupFailureProps {
    connectionsQueryKey: Array<string>;
    nodeName: string | undefined;
    workflowIssues: Array<WorkflowIssueI>;
}

export default function useRefreshAfterLookupFailure({
    connectionsQueryKey,
    nodeName,
    workflowIssues,
}: UseRefreshAfterLookupFailureProps) {
    const queryClient = useQueryClient();

    const nodeLookupFailures = useMemo(
        () =>
            workflowIssues
                .filter(
                    (workflowIssue) => workflowIssue.kind === 'LOOKUP_FAILED' && workflowIssue.nodeName === nodeName
                )
                .map((workflowIssue) => `${workflowIssue.propertyPath ?? ''}|${workflowIssue.message}`)
                .join(','),
        [nodeName, workflowIssues]
    );

    useEffect(() => {
        if (nodeLookupFailures) {
            void queryClient.invalidateQueries({queryKey: connectionsQueryKey});

            invalidateWorkflowValidation(queryClient);
        }
    }, [connectionsQueryKey, nodeLookupFailures, queryClient]);
}
