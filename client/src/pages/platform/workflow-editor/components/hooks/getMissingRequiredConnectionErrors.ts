import {ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {ComponentConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';

import {WorkflowIssueI, WorkflowIssueKindType, WorkflowIssueSeverityType} from '../../stores/useWorkflowIssuesStore';

export interface WorkflowNodeDetailsErrorI {
    kind: 'CONNECTION' | 'INVALID_CONNECTION' | 'ISSUE' | 'PROPERTY';
    name: string;
    propertyLabel?: string;
    severity: WorkflowIssueSeverityType;
}

type NodeIssueType = Pick<WorkflowIssueI, 'kind' | 'message' | 'propertyPath' | 'referencedNodeName' | 'severity'>;

interface GetMissingRequiredConnectionErrorsProps {
    clusterRoot?: boolean;
    componentTitle?: string;
    connections: Array<ComponentConnection>;
    workflowTestConfigurationConnections?: Array<WorkflowTestConfigurationConnection>;
}

interface GetInvalidConnectionErrorsProps {
    availableConnections?: Array<Pick<ConnectionI, 'credentialStatus' | 'id' | 'name'>>;
    connections: Array<ComponentConnection>;
    workflowTestConfigurationConnections?: Array<WorkflowTestConfigurationConnection>;
}

const SEPARATELY_REPORTED_ISSUE_KINDS = new Set<WorkflowIssueKindType>([
    'INVALID_CONNECTION',
    'MISSING_CONNECTION',
    'MISSING_REQUIRED',
]);

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
            severity: 'ERROR',
        }));
}

/**
 * The server marks a connection INVALID when refreshing its credentials fails, so a selected connection in that state
 * cannot be used until it is reconnected.
 */
export function getInvalidConnectionErrors({
    availableConnections,
    connections,
    workflowTestConfigurationConnections,
}: GetInvalidConnectionErrorsProps): Array<WorkflowNodeDetailsErrorI> {
    if (!availableConnections || !workflowTestConfigurationConnections) {
        return [];
    }

    return workflowTestConfigurationConnections
        .filter((testConfigurationConnection) =>
            connections.some((connection) => connection.key === testConfigurationConnection.workflowConnectionKey)
        )
        .map((testConfigurationConnection) =>
            availableConnections.find(
                (availableConnection) => availableConnection.id === testConfigurationConnection.connectionId
            )
        )
        .filter((availableConnection) => availableConnection?.credentialStatus === 'INVALID')
        .map((availableConnection) => ({
            kind: 'INVALID_CONNECTION',
            name: availableConnection!.name,
            severity: 'ERROR',
        }));
}

/**
 * Missing required properties and connections are resolved for the open node directly, so their errors are not listed
 * again from the node issues. Warnings of those kinds (e.g. a missing recommended field) have no other source and stay.
 */
export function getWorkflowIssueErrors(
    nodeIssues: Array<NodeIssueType>,
    getPropertyLabels: (nodeIssue: NodeIssueType) => Array<string> = () => []
): Array<WorkflowNodeDetailsErrorI> {
    const errorsByKey = new Map<string, WorkflowNodeDetailsErrorI>();

    for (const nodeIssue of nodeIssues) {
        if (nodeIssue.severity === 'ERROR' && SEPARATELY_REPORTED_ISSUE_KINDS.has(nodeIssue.kind)) {
            continue;
        }

        const propertyLabel = [...new Set(getPropertyLabels(nodeIssue))].join(', ') || undefined;
        const key = `${propertyLabel ?? ''}|${nodeIssue.message}`;

        if (errorsByKey.get(key)?.severity !== 'ERROR') {
            errorsByKey.set(key, {kind: 'ISSUE', name: nodeIssue.message, propertyLabel, severity: nodeIssue.severity});
        }
    }

    return [...errorsByKey.values()];
}

export function getWorkflowNodeDetailsErrorsSummary(errors: Array<Pick<WorkflowNodeDetailsErrorI, 'severity'>>): {
    heading: string;
    warningOnly: boolean;
} {
    const errorCount = errors.filter((error) => error.severity === 'ERROR').length;
    const warningCount = errors.length - errorCount;

    const headingParts: Array<string> = [];

    if (errorCount > 0) {
        headingParts.push(`Errors (${errorCount})`);
    }

    if (warningCount > 0) {
        headingParts.push(`Warnings (${warningCount})`);
    }

    return {heading: headingParts.join(', '), warningOnly: errors.length > 0 && errorCount === 0};
}
