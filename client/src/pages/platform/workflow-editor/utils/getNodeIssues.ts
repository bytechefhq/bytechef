import {WorkflowIssueI} from '../stores/useWorkflowIssuesStore';
import getWorkflowIssueOwnerName from './getWorkflowIssueOwnerName';

interface GetNodeIssuesProps {
    clusterElementRootNames: ReadonlyMap<string, string>;
    includeClusterElementIssues?: boolean;
    issues: Array<WorkflowIssueI>;
    nodeName: string;
}

/**
 * Returns the issues that belong to a node. A cluster root also gets the missing connections of its cluster elements,
 * because they are configured on the cluster root, and every cluster element issue when it is shown collapsed.
 */
export default function getNodeIssues({
    clusterElementRootNames,
    includeClusterElementIssues = false,
    issues,
    nodeName,
}: GetNodeIssuesProps): Array<WorkflowIssueI> {
    return issues.filter((issue) => {
        const ownerName = getWorkflowIssueOwnerName(issue, clusterElementRootNames);

        if (ownerName === nodeName) {
            return true;
        }

        if (clusterElementRootNames.get(ownerName) !== nodeName) {
            return false;
        }

        return includeClusterElementIssues || issue.kind === 'MISSING_CONNECTION';
    });
}
