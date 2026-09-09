import {useMemo} from 'react';

import {WorkflowIssueSeverityType} from '../stores/useWorkflowIssuesStore';
import useWorkflowIssues from './useWorkflowIssues';

export default function useNodeIssues(
    nodeName: string,
    clusterElement = false
): {
    count: number;
    severity?: WorkflowIssueSeverityType;
    title?: string;
} {
    const issues = useWorkflowIssues();

    return useMemo(() => {
        const nodeIssues = issues.filter(
            (issue) =>
                issue.nodeName === nodeName ||
                (clusterElement &&
                    !!issue.propertyPath &&
                    (issue.propertyPath.startsWith(`${nodeName}.`) || issue.propertyPath.includes(`.${nodeName}.`)))
        );

        return {
            count: nodeIssues.length,
            severity: nodeIssues[0]?.severity,
            title: nodeIssues.map((issue) => issue.message).join('\n') || undefined,
        };
    }, [clusterElement, issues, nodeName]);
}
