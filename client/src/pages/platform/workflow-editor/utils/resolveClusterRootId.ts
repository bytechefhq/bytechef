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
