interface WorkflowNodeOperationI {
    operationName?: string;
    workflowNodeName?: string;
}

interface ResolveSyncedOperationNameProps {
    clusterElementComponentOperations?: WorkflowNodeOperationI[];
    currentNode?: WorkflowNodeOperationI;
    isClusterElement: boolean;
    operationChangeInProgress: boolean;
    workflowNodes?: WorkflowNodeOperationI[];
}

export default function resolveSyncedOperationName({
    clusterElementComponentOperations,
    currentNode,
    isClusterElement,
    operationChangeInProgress,
    workflowNodes,
}: ResolveSyncedOperationNameProps): string | undefined {
    if (operationChangeInProgress) {
        return undefined;
    }

    let currentWorkflowNode: WorkflowNodeOperationI | undefined;

    if (workflowNodes?.length && !isClusterElement) {
        currentWorkflowNode = workflowNodes.find(
            (workflowNode) => workflowNode.workflowNodeName === currentNode?.workflowNodeName
        );
    } else if (isClusterElement && clusterElementComponentOperations) {
        currentWorkflowNode = clusterElementComponentOperations.find(
            (workflowNodeType) => workflowNodeType.workflowNodeName === currentNode?.workflowNodeName
        );
    }

    // Prefer the server-synced operationName once available; otherwise fall back to the
    // optimistically-set currentNode.operationName so a newly added node can resolve its
    // operation before the workflow mutation settles.
    return currentWorkflowNode?.operationName ?? currentNode?.operationName ?? '';
}
