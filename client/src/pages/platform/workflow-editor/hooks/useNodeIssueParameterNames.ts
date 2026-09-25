import {useMemo} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {getIssueParameterNames} from '../utils/findWorkflowIssueParameterPaths';
import getNodeIssues from '../utils/getNodeIssues';
import {getClusterElementRootNames} from '../utils/getWorkflowIssueOwnerName';
import useWorkflowIssues from './useWorkflowIssues';

export default function useNodeIssueParameterNames(
    nodeName: string | undefined,
    parameters: Record<string, unknown> | undefined
): Set<string> {
    const issues = useWorkflowIssues();

    const tasks = useWorkflowDataStore((state) => state.workflow.tasks);

    const clusterElementRootNames = useMemo(() => getClusterElementRootNames(tasks), [tasks]);

    return useMemo(() => {
        if (!nodeName) {
            return new Set<string>();
        }

        return getIssueParameterNames(
            getNodeIssues({clusterElementRootNames, issues, nodeName}),
            parameters,
            clusterElementRootNames
        );
    }, [clusterElementRootNames, issues, nodeName, parameters]);
}
