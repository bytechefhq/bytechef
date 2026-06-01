import {NodeDataType} from '@/shared/types';

interface OperationWithDescriptionI {
    description?: string;
    name: string;
}

interface GetNodeOperationDescriptionProps {
    actionDescription?: string;
    clusterElementOperations?: Array<OperationWithDescriptionI>;
    currentNode?: NodeDataType;
    currentOperationName: string;
    rootClusterElementWorkflowNodeName?: string;
    triggerDescription?: string;
}

export default function getNodeOperationDescription({
    actionDescription,
    clusterElementOperations,
    currentNode,
    currentOperationName,
    rootClusterElementWorkflowNodeName,
    triggerDescription,
}: GetNodeOperationDescriptionProps): string | undefined {
    if (currentNode?.trigger) {
        return triggerDescription;
    }

    const isNonRootClusterElement =
        !!currentNode?.clusterElementType && currentNode.workflowNodeName !== rootClusterElementWorkflowNodeName;

    if (isNonRootClusterElement) {
        return clusterElementOperations?.find((operation) => operation.name === currentOperationName)?.description;
    }

    return actionDescription;
}
