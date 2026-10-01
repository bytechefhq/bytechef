import {NodeDataType} from '@/shared/types';

export function resolveClusterRootId(data: NodeDataType): string | undefined {
    if (data.topLevelClusterRootId) {
        return data.topLevelClusterRootId;
    }

    if (data.parentClusterRootId) {
        return data.parentClusterRootId;
    }

    return data.clusterRoot ? data.workflowNodeName : undefined;
}

export function resolveMainClusterRootName(
    currentNode: NodeDataType | undefined,
    rootClusterElementNodeData: NodeDataType | undefined
): string | undefined {
    const resolvedFromNode = currentNode ? resolveClusterRootId(currentNode) : undefined;

    if (resolvedFromNode) {
        return resolvedFromNode;
    }

    return currentNode?.clusterElementType ? rootClusterElementNodeData?.workflowNodeName : undefined;
}
