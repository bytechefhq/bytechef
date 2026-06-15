import {ComponentConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';

type NodeConnectionInfoType = {
    clusterElementType?: string;
    connections?: Array<ComponentConnection>;
    workflowNodeName?: string;
};

type ResolveNodeConnectionFieldsOptionsType = {
    rootClusterElementWorkflowNodeName?: string;
    workflowTestConfigurationConnections?: Array<WorkflowTestConfigurationConnection>;
};

type ResolvedNodeConnectionFieldsType = {
    connectionId?: number;
    connections: Array<ComponentConnection>;
};

export default function resolveNodeConnectionFields(
    currentNode: NodeConnectionInfoType,
    currentWorkflowNodeConnections: Array<ComponentConnection>,
    {rootClusterElementWorkflowNodeName, workflowTestConfigurationConnections}: ResolveNodeConnectionFieldsOptionsType
): ResolvedNodeConnectionFieldsType | null {
    if (currentWorkflowNodeConnections.length) {
        if (currentNode.clusterElementType) {
            const connectionId = workflowTestConfigurationConnections?.find(
                (connection) => connection.workflowConnectionKey === currentNode.workflowNodeName
            )?.connectionId;

            return {connectionId, connections: currentWorkflowNodeConnections};
        }

        if (currentNode.workflowNodeName === rootClusterElementWorkflowNodeName) {
            const connectionId = workflowTestConfigurationConnections?.find(
                (connection) =>
                    connection.workflowNodeName === rootClusterElementWorkflowNodeName &&
                    currentWorkflowNodeConnections.some(
                        (curConnection) => curConnection.key === connection.workflowConnectionKey
                    )
            )?.connectionId;

            return {connectionId, connections: currentWorkflowNodeConnections};
        }

        return {
            connectionId: workflowTestConfigurationConnections
                ? workflowTestConfigurationConnections[0]?.connectionId
                : undefined,
            connections: currentWorkflowNodeConnections,
        };
    }

    if (currentNode.connections === undefined) {
        return {connections: []};
    }

    return null;
}
