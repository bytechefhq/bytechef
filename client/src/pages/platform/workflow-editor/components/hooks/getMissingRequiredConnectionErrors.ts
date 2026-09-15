import {ComponentConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';

import {WorkflowIssueI, WorkflowIssueKindType} from '../../stores/useWorkflowIssuesStore';

export interface WorkflowNodeDetailsErrorI {
    kind: 'CONNECTION' | 'ISSUE' | 'PROPERTY';
    name: string;
}

interface GetMissingRequiredConnectionErrorsProps {
    clusterRoot?: boolean;
    componentTitle?: string;
    connections: Array<ComponentConnection>;
    workflowTestConfigurationConnections?: Array<WorkflowTestConfigurationConnection>;
}

const SEPARATELY_REPORTED_ISSUE_KINDS = new Set<WorkflowIssueKindType>(['MISSING_CONNECTION', 'MISSING_REQUIRED']);

/**
 * A cluster root lists its own connection (keyed by its component name) together with the connections of its cluster
 * elements (keyed by the cluster element name), so only there does the key name the entry.
 */
export default function getMissingRequiredConnectionErrors({
    clusterRoot = false,
    componentTitle,
    connections,
    workflowTestConfigurationConnections,
}: GetMissingRequiredConnectionErrorsProps): Array<WorkflowNodeDetailsErrorI> {
    if (!workflowTestConfigurationConnections) {
        return [];
    }

    return connections
        .filter(
            (connection) =>
                connection.required &&
                !workflowTestConfigurationConnections.some(
                    (testConfigurationConnection) =>
                        testConfigurationConnection.workflowConnectionKey === connection.key
                )
        )
        .map((connection) => ({
            kind: 'CONNECTION',
            name:
                clusterRoot && connection.key !== connection.componentName
                    ? connection.key
                    : componentTitle || connection.componentName,
        }));
}

/**
 * Missing required properties and connections are resolved for the open node directly, so only the remaining workflow
 * issue kinds (broken references, type mismatches, failed lookups, ...) are listed from the node issues.
 */
export function getWorkflowIssueErrors(
    nodeIssues: Array<Pick<WorkflowIssueI, 'kind' | 'message'>>
): Array<WorkflowNodeDetailsErrorI> {
    const messages = new Set<string>();

    for (const nodeIssue of nodeIssues) {
        if (!SEPARATELY_REPORTED_ISSUE_KINDS.has(nodeIssue.kind)) {
            messages.add(nodeIssue.message);
        }
    }

    return [...messages].map((message) => ({kind: 'ISSUE', name: message}));
}
