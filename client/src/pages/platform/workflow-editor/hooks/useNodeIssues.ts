import {useMemo} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {WorkflowIssueSeverityType} from '../stores/useWorkflowIssuesStore';
import getNodeIssues from '../utils/getNodeIssues';
import {getClusterElementRootNames} from '../utils/getWorkflowIssueOwnerName';
import useWorkflowIssues from './useWorkflowIssues';

export default function useNodeIssues(
    nodeName: string,
    includeClusterElementIssues = false
): {
    count: number;
    severity?: WorkflowIssueSeverityType;
    title?: string;
} {
    const issues = useWorkflowIssues();

    const tasks = useWorkflowDataStore((state) => state.workflow.tasks);

    const clusterElementRootNames = useMemo(() => getClusterElementRootNames(tasks), [tasks]);

    return useMemo(() => {
        const nodeIssues = getNodeIssues({clusterElementRootNames, includeClusterElementIssues, issues, nodeName});

        return {
            count: nodeIssues.length,
            severity: nodeIssues[0]?.severity,
            title: nodeIssues.map((issue) => issue.message).join('\n') || undefined,
        };
    }, [clusterElementRootNames, includeClusterElementIssues, issues, nodeName]);
}
