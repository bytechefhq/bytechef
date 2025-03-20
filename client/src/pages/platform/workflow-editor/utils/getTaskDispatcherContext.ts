import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {NodeDataType, TaskDispatcherContextType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

/**
 * Creates context from a task node's dispatcher data
 */
function getContextFromTaskNodeData(nodeData: NodeDataType, indexIncrement: number = 0): TaskDispatcherContextType {
    const context: TaskDispatcherContextType = {};

    if (!nodeData) {
        return context;
    }

    if (nodeData.conditionData) {
        context.conditionCase = nodeData.conditionData.conditionCase as string;
        context.conditionId = nodeData.conditionData.conditionId as string;
        context.index = (nodeData.conditionData.index as number) + indexIncrement;
    } else if (nodeData.loopData) {
        context.loopId = nodeData.loopData.loopId as string;
        context.index = (nodeData.loopData.index as number) + indexIncrement;
    }

    return context;
}

/**
 * Creates context from a placeholder node
 */
function getContextFromPlaceholderNode(node: Node): TaskDispatcherContextType {
    const context: TaskDispatcherContextType = {};

    if (!node) {
        return context;
    }

    if (node.id.includes('loop-placeholder')) {
        const loopId = node.id.split('-loop-placeholder')[0];
        const placeholderIndex = parseInt(node.id.split('-').pop() || '0');

        context.loopId = loopId;
        context.index = placeholderIndex;
    } else if (node.id.includes('condition-placeholder')) {
        context.conditionId = node.data?.conditionId as string;
        context.conditionCase = node.data?.conditionCase as string;
        context.index = 0;
    } else {
        context.conditionCase = node.data?.conditionCase as string;
        context.conditionId = node.data?.conditionId as string;
        context.loopId = node.data?.loopId as string;
        context.index = 0;
    }

    return context;
}

export default function getTaskDispatcherContext({
    edge,
    node,
    nodes,
}: {
    edge?: Edge;
    node?: Node;
    nodes?: Array<Node>;
}): TaskDispatcherContextType {
    if (node) {
        return getContextFromPlaceholderNode(node);
    }

    if (edge && nodes) {
        const {source, target} = edge;

        const sourceNodeComponentName = source.split('_')[0];
        const targetNodeComponentName = target.split('_')[0];

        const isSourceTaskDispatcher = TASK_DISPATCHER_NAMES.includes(sourceNodeComponentName);
        const isTargetTaskDispatcher = TASK_DISPATCHER_NAMES.includes(targetNodeComponentName);

        const isSourceGhost = source.includes('ghost');
        const isTargetGhost = target.includes('ghost');

        // Case 1: Ghost node to ghost node
        if (isSourceGhost && isTargetGhost) {
            const sourceNode = nodes.find((node) => node.id === source);

            if (!sourceNode) {
                return {};
            }

            const sourceTaskDispatcherNode = nodes.find((node) => node.id === sourceNode.data.taskDispatcherId);

            if (!sourceTaskDispatcherNode) {
                return {};
            }

            return getContextFromTaskNodeData(sourceTaskDispatcherNode.data as NodeDataType, 1);
        }

        // Case 2: Task dispatcher to task node
        if (isSourceTaskDispatcher) {
            const targetNode = nodes.find((node) => node.id === target);

            if (!targetNode) {
                return {};
            }

            return getContextFromTaskNodeData(targetNode.data as NodeDataType, 0);
        }

        // Case 3: Task node to ghost node
        if (isTargetGhost) {
            const sourceNode = nodes.find((node) => node.id === source);

            if (!sourceNode) {
                return {};
            }

            return getContextFromTaskNodeData(sourceNode.data as NodeDataType, 1);
        }

        // Case 4: Task node to task node
        if (!isSourceTaskDispatcher && !isTargetTaskDispatcher) {
            const sourceNode = nodes.find((node) => node.id === source);

            if (!sourceNode) {
                return {};
            }

            return getContextFromTaskNodeData(sourceNode.data as NodeDataType, 1);
        }

        // Case 5: Task node to task dispatcher
        if (!isSourceTaskDispatcher && isTargetTaskDispatcher) {
            const sourceNode = nodes.find((node) => node.id === source);

            if (!sourceNode) {
                return {};
            }

            return getContextFromTaskNodeData(sourceNode.data as NodeDataType, 1);
        }
    }

    return {};
}
