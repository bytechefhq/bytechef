import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {NodeDataType, TaskDispatcherContextType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

/**
 * Creates context from a task node's dispatcher data
 */
function getContextFromTaskNodeData(nodeData: NodeDataType, indexIncrement: number = 0): TaskDispatcherContextType {
    const context: TaskDispatcherContextType = {
        taskDispatcherId: nodeData.taskDispatcherId as string,
    };

    if (!nodeData) {
        return context;
    }

    if (nodeData.conditionData) {
        context.conditionCase = nodeData.conditionData.conditionCase as string;
        context.conditionId = nodeData.conditionData.conditionId as string;
        context.index = (nodeData.conditionData.index as number) + indexIncrement;
        context.taskDispatcherId = nodeData.conditionData.conditionId as string;
    } else if (nodeData.loopData) {
        context.loopId = nodeData.loopData.loopId as string;
        context.index = (nodeData.loopData.index as number) + indexIncrement;
        context.taskDispatcherId = nodeData.loopData.loopId as string;
    }

    return context;
}

/**
 * Creates context from a placeholder node
 */
function getContextFromPlaceholderNode(node: Node): TaskDispatcherContextType {
    const context: TaskDispatcherContextType = {
        taskDispatcherId: node.data?.taskDispatcherId as string,
    };

    if (!node) {
        return context;
    }

    if (node.id.includes('loop-placeholder')) {
        const loopId = node.id.split('-loop-placeholder')[0];
        const placeholderIndex = parseInt(node.id.split('-').pop() || '0');

        context.loopId = loopId;
        context.index = placeholderIndex;
        context.taskDispatcherId = loopId;
    } else if (node.id.includes('condition-placeholder')) {
        context.conditionId = node.data?.conditionId as string;
        context.conditionCase = node.data?.conditionCase as string;
        context.index = 0;
        context.taskDispatcherId = node.data?.conditionId as string;
    } else {
        context.conditionCase = node.data?.conditionCase as string;
        context.conditionId = node.data?.conditionId as string;
        context.loopId = node.data?.loopId as string;
        context.index = 0;
        context.taskDispatcherId = node.data?.conditionId as string;
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

    const {source, target} = edge!;

    const sourceNode = nodes!.find((node) => node.id === source);
    const targetNode = nodes!.find((node) => node.id === target);

    const isSourceTaskDispatcher = TASK_DISPATCHER_NAMES.includes((sourceNode?.data as NodeDataType).componentName);
    const isTargetTaskDispatcher = TASK_DISPATCHER_NAMES.includes((targetNode?.data as NodeDataType).componentName);

    const isSourceTask = sourceNode?.type === 'workflow';
    const isTargetTask = targetNode?.type === 'workflow';

    const isSourceGhost = source.includes('ghost');
    const isTargetGhost = target.includes('ghost');

    const taskDispatcherId = (sourceNode?.data?.taskDispatcherId || targetNode?.data.taskDispatcherId) as string;

    const context: TaskDispatcherContextType = {
        taskDispatcherId,
    };

    if (!sourceNode || !targetNode) {
        return context;
    }

    if (isSourceGhost && isTargetGhost) {
        const sourceTaskDispatcherNode = nodes!.find((node) => node.id === sourceNode.data.taskDispatcherId);

        return getContextFromTaskNodeData(sourceTaskDispatcherNode?.data as NodeDataType, 1);
    }

    if (isSourceTaskDispatcher && isTargetTask) {
        return getContextFromTaskNodeData(targetNode.data as NodeDataType, 0);
    }

    if (isSourceTask && isTargetGhost) {
        return getContextFromTaskNodeData(sourceNode.data as NodeDataType, 1);
    }

    if (isSourceTask && isTargetTask) {
        return getContextFromTaskNodeData(sourceNode.data as NodeDataType, 1);
    }

    if (isSourceTask && isTargetTaskDispatcher) {
        return getContextFromTaskNodeData(targetNode.data as NodeDataType, 1);
    }

    if (isSourceGhost && isTargetTask) {
        return getContextFromTaskNodeData(targetNode.data as NodeDataType);
    }

    return context;
}
