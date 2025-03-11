import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {NodeDataType, TaskDispatcherContextType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

export default function getTaskDispatcherContext({
    edge,
    node,
    nodes,
}: {
    edge?: Edge;
    node?: Node;
    nodes?: Array<Node>;
}): TaskDispatcherContextType {
    const taskDispatcherContext: TaskDispatcherContextType = {};

    if (edge) {
        const {source, target} = edge;

        const sourceNodeComponentName = source.split('_')[0];
        const targetNodeComponentName = target.split('_')[0];

        const isSourceTaskDispatcher = TASK_DISPATCHER_NAMES.includes(sourceNodeComponentName);
        const isTargetTaskDispatcher = TASK_DISPATCHER_NAMES.includes(targetNodeComponentName);

        const isSourceTaskDispatcherBottomGhostNode = source.includes('bottom-ghost');
        const isTargetTaskDispatcherBottomGhostNode = target.includes('bottom-ghost');

        if (isSourceTaskDispatcherBottomGhostNode && isTargetTaskDispatcherBottomGhostNode && nodes) {
            const sourceNode = nodes.find((node) => node.id === source);

            if (!sourceNode) {
                return taskDispatcherContext;
            }

            const sourceTaskDispatcherNode = nodes.find((node) => node.id === sourceNode?.data.taskDispatcherId);

            if (!sourceTaskDispatcherNode) {
                return taskDispatcherContext;
            }

            const taskDispatcherData = sourceTaskDispatcherNode.data as NodeDataType;

            taskDispatcherContext.conditionId = taskDispatcherData.conditionData?.conditionId as string;
            taskDispatcherContext.conditionCase = taskDispatcherData.conditionData?.conditionCase as string;
            taskDispatcherContext.index = (taskDispatcherData.conditionData?.index as number) + 1;

            return taskDispatcherContext;
        }

        if (isSourceTaskDispatcher) {
            if (!nodes) {
                return taskDispatcherContext;
            }

            const targetNodeData = nodes.find((node) => node.id === target)?.data as NodeDataType;

            if (targetNodeData.conditionData) {
                taskDispatcherContext.conditionCase = targetNodeData.conditionData!.conditionCase as string;
                taskDispatcherContext.conditionId = targetNodeData.conditionData!.conditionId as string;
                taskDispatcherContext.index = targetNodeData.conditionData!.index as number;
            }

            return taskDispatcherContext;
        }

        if (isTargetTaskDispatcherBottomGhostNode) {
            const sourceNodeData = nodes?.find((node) => node.id === source)?.data as NodeDataType;

            if (sourceNodeData.conditionData) {
                taskDispatcherContext.conditionCase = sourceNodeData.conditionData.conditionCase as string;
                taskDispatcherContext.conditionId = sourceNodeData.conditionData.conditionId as string;
                taskDispatcherContext.index = (sourceNodeData.conditionData.index as number) + 1;
            }

            return taskDispatcherContext;
        }

        if (!isSourceTaskDispatcher && !isTargetTaskDispatcher) {
            const sourceNodeData = nodes?.find((node) => node.id === source)?.data as NodeDataType;

            if (sourceNodeData.conditionData) {
                taskDispatcherContext.conditionCase = sourceNodeData.conditionData.conditionCase as string;
                taskDispatcherContext.conditionId = sourceNodeData.conditionData.conditionId as string;
                taskDispatcherContext.index = (sourceNodeData.conditionData.index as number) + 1;
            }
        }
    } else if (node) {
        taskDispatcherContext.conditionCase = node.data?.conditionCase as string;
        taskDispatcherContext.conditionId = node.data?.conditionId as string;
        taskDispatcherContext.index = 0;
    }

    return taskDispatcherContext;
}
