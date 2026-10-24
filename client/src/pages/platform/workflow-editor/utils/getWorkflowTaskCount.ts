import {NodeDataType} from '@/shared/types';
import {type Node} from '@xyflow/react';

const TASK_NODE_TYPES = ['clusterRoot', 'readonly', 'workflow'];

export default function getWorkflowTaskCount(nodes: Node[]): number {
    return nodes.filter((node) => {
        const data = node.data as NodeDataType;

        if (!node.type || !TASK_NODE_TYPES.includes(node.type) || !data) {
            return false;
        }

        return !data.trigger && !data.taskDispatcher && !data.clusterElementType && !data.isNestedClusterRoot;
    }).length;
}
