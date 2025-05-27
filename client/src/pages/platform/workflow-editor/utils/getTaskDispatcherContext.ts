import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {NodeDataType, TaskDispatcherContextType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

/**
 * Creates context from a task node's dispatcher data
 */
function getContextFromTaskNodeData(
    nodeData: NodeDataType,
    indexIncrement: number = 0
): TaskDispatcherContextType | undefined {
    const context: TaskDispatcherContextType = {
        taskDispatcherId: nodeData.taskDispatcherId as string,
    };

    if (!nodeData) {
        return undefined;
    }

    if (nodeData.conditionData) {
        context.conditionCase = nodeData.conditionData.conditionCase as 'caseTrue' | 'caseFalse';
        context.conditionId = nodeData.conditionData.conditionId as string;
        context.index = (nodeData.conditionData.index as number) + indexIncrement;
        context.taskDispatcherId = nodeData.conditionData.conditionId as string;
    } else if (nodeData.loopData) {
        context.loopId = nodeData.loopData.loopId as string;
        context.index = (nodeData.loopData.index as number) + indexIncrement;
        context.taskDispatcherId = nodeData.loopData.loopId as string;
    } else if (nodeData.branchData) {
        context.branchId = nodeData.branchData.branchId as string;
        context.caseKey = nodeData.branchData.caseKey as string;
        context.index = (nodeData.branchData.index as number) + indexIncrement;
        context.taskDispatcherId = nodeData.branchData.branchId as string;
    } else if (nodeData.parallelData) {
        context.parallelId = nodeData.parallelData.parallelId as string;
        context.index = (nodeData.parallelData.index as number) + indexIncrement;
        context.taskDispatcherId = nodeData.parallelData.parallelId as string;
    } else if (nodeData.eachData) {
        context.eachId = nodeData.eachData.eachId as string;
        context.index = (nodeData.eachData.index as number) + indexIncrement;
        context.taskDispatcherId = nodeData.eachData.eachId as string;
    }

    return context;
}

/**
 * Creates context from a placeholder node
 */
function getContextFromPlaceholderNode(placeholderNode: Node): TaskDispatcherContextType | undefined {
    const isPlaceholder = placeholderNode.type === 'placeholder';
    const isLoopPlaceholder = placeholderNode.id.includes('loop') && isPlaceholder;
    const isConditionPlaceholder = placeholderNode.id.includes('condition') && isPlaceholder;
    const isBranchPlaceholder = placeholderNode.id.includes('branch') && isPlaceholder;
    const isParallelPlaceholder = placeholderNode.id.includes('parallel') && isPlaceholder;
    const isEachPlaceholder = placeholderNode.id.includes('each') && isPlaceholder;

    const context: TaskDispatcherContextType = {
        taskDispatcherId: placeholderNode.data?.taskDispatcherId as string,
    };

    if (!placeholderNode) {
        return undefined;
    }

    const placeholderIndex = parseInt(placeholderNode.id.split('-').pop() || '0');

    context.index = placeholderIndex;

    if (isLoopPlaceholder) {
        const loopId = placeholderNode.data.loopId as string;

        context.loopId = loopId;
        context.taskDispatcherId = loopId;
    } else if (isConditionPlaceholder) {
        const conditionId = placeholderNode.data.conditionId as string;

        context.conditionId = conditionId;
        context.conditionCase = placeholderNode.data?.conditionCase as 'caseTrue' | 'caseFalse';
        context.taskDispatcherId = conditionId;
    } else if (isBranchPlaceholder) {
        const branchId = placeholderNode.data.branchId as string;

        context.branchId = branchId;
        context.caseKey = placeholderNode.data?.caseKey as string;
        context.taskDispatcherId = branchId;
    } else if (isParallelPlaceholder) {
        const parallelId = placeholderNode.data?.parallelId as string;

        context.parallelId = parallelId;
        context.taskDispatcherId = parallelId;
    } else if (isEachPlaceholder) {
        const eachId = placeholderNode.data?.eachId as string;

        context.eachId = eachId;
        context.taskDispatcherId = eachId;
    }

    return context;
}

interface GetTaskDispatcherContextProps {
    edge?: Edge;
    node?: Node;
    nodes?: Array<Node>;
}

export default function getTaskDispatcherContext({
    edge,
    node,
    nodes,
}: GetTaskDispatcherContextProps): TaskDispatcherContextType | undefined {
    if (node) {
        return getContextFromPlaceholderNode(node);
    }

    const {source, target} = edge!;

    const sourceNode = nodes!.find((node) => node.id === source);
    const targetNode = nodes!.find((node) => node.id === target);

    const isSourceTaskDispatcher = TASK_DISPATCHER_NAMES.includes((sourceNode?.data as NodeDataType).componentName);
    const isTargetTaskDispatcher = TASK_DISPATCHER_NAMES.includes((targetNode?.data as NodeDataType).componentName);

    const isSourceTask = sourceNode?.type === 'workflow' && !isSourceTaskDispatcher;
    const isTargetTask = targetNode?.type === 'workflow' && !isTargetTaskDispatcher;

    const isSourceGhost = source.includes('ghost');
    const isTargetGhost = target.includes('ghost');

    const isSourceTopGhost = sourceNode?.type === 'taskDispatcherTopGhostNode';

    const isSourceNestedBottomGhost = sourceNode?.data.isNestedBottomGhost;

    const taskDispatcherId = (sourceNode?.data?.taskDispatcherId || targetNode?.data.taskDispatcherId) as string;

    const context: TaskDispatcherContextType = {
        taskDispatcherId,
    };

    if (!sourceNode || !targetNode) {
        return undefined;
    }

    if (isSourceGhost && isTargetGhost) {
        const sourceTaskDispatcherNode = nodes!.find((node) => node.id === sourceNode.data.taskDispatcherId);

        return getContextFromTaskNodeData(sourceTaskDispatcherNode?.data as NodeDataType, 1);
    }

    if (isSourceGhost && isTargetTaskDispatcher) {
        if (!isSourceNestedBottomGhost) {
            if (
                targetNode.data.conditionData ||
                targetNode.data.loopData ||
                targetNode.data.branchData ||
                targetNode.data.parallelData ||
                targetNode.data.eachData
            ) {
                return getContextFromTaskNodeData(targetNode.data as NodeDataType, 0);
            }

            return undefined;
        }

        return getContextFromTaskNodeData(targetNode.data as NodeDataType, 0);
    }

    if (isSourceTopGhost && isTargetTask) {
        return getContextFromTaskNodeData(targetNode.data as NodeDataType, 0);
    }

    if (isSourceGhost && !isSourceNestedBottomGhost && isTargetTask) {
        return getContextFromTaskNodeData(targetNode.data as NodeDataType, 1);
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
        if (!sourceNode.data.conditionData && !sourceNode.data.loopData && !sourceNode.data.branchData) {
            return undefined;
        }

        return getContextFromTaskNodeData(targetNode.data as NodeDataType, 0);
    }

    return context;
}
