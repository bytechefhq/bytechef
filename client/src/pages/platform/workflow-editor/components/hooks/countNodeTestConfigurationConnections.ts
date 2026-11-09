import {WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';

interface CountNodeTestConfigurationConnectionsProps {
    clusterElementType?: string;
    workflowNodeName?: string;
    workflowTestConfigurationConnections?: Array<WorkflowTestConfigurationConnection>;
}

export default function countNodeTestConfigurationConnections({
    clusterElementType,
    workflowNodeName,
    workflowTestConfigurationConnections,
}: CountNodeTestConfigurationConnectionsProps): number {
    if (!workflowTestConfigurationConnections) {
        return 0;
    }

    if (clusterElementType) {
        return workflowTestConfigurationConnections.filter(
            (connection) => connection.workflowConnectionKey === workflowNodeName
        ).length;
    }

    return workflowTestConfigurationConnections.length;
}
