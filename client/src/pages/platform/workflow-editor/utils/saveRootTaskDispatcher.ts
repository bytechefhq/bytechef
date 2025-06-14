import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {
    NodeDataType,
    TaskDispatcherContextType,
    TaskDispatcherDataType,
    UpdateWorkflowMutationType,
} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {Node} from '@xyflow/react';

import saveWorkflowDefinition from './saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

interface SaveRootTaskDispatcherProps {
    invalidateWorkflowQueries: () => void;
    nodes: Node[];
    parentNodeData: NodeDataType;
    queryClient: QueryClient;
    updatedParameters: object;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowDefinition: string;
}

/**
 * Updates a task dispatcher node and propagates changes up through the workflow hierarchy
 * regardless of the task dispatcher types in the parent chain.
 */
export default function saveRootTaskDispatcher({
    invalidateWorkflowQueries,
    nodes,
    parentNodeData,
    updateWorkflowMutation,
    updatedParameters,
    workflowDefinition,
}: SaveRootTaskDispatcherProps): void {
    if (!workflowDefinition) {
        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflowDefinition).tasks;

    const nodesMap: Map<string, Node> = new Map();

    nodes.forEach((node) => {
        const {data, id} = node;

        nodesMap.set(id, {
            ...node,
            data: {
                ...data,
                ...TASK_DISPATCHER_NAMES.reduce((nodeData: {[key: string]: unknown}, taskDispatcherName) => {
                    const key = `${taskDispatcherName}Data`;

                    if (data[key]) {
                        nodeData[key] = data[key];
                    }

                    return nodeData;
                }, {}),
            } as NodeDataType,
            id,
        });
    });

    const taskNode = nodes.find((node) => node.id === parentNodeData?.name || node.data.name === parentNodeData?.name);

    if (!taskNode) {
        return;
    }

    const updatedTaskNodeData: NodeDataType = {
        ...(taskNode.data as NodeDataType),
        parameters: updatedParameters,
    };

    const rootNodeData = getRecursivelyUpdatedRootTaskDispatcherNodeData(
        updatedTaskNodeData,
        workflowDefinitionTasks,
        nodesMap
    );

    saveWorkflowDefinition({
        invalidateWorkflowQueries,
        nodeData: rootNodeData,
        updateWorkflowMutation,
    });
}

/**
 * Recursively traverses the task hierarchy, updating tasks along the way.
 * Carefully manages references to avoid circular structures.
 */
function getRecursivelyUpdatedRootTaskDispatcherNodeData(
    currentTaskNodeData: NodeDataType,
    definitionTasks: WorkflowTask[],
    nodesMap: Map<string, Node>
): NodeDataType {
    const taskName = currentTaskNodeData.name || currentTaskNodeData.workflowNodeName;
    const nodeData: NodeDataType = (nodesMap.get(taskName)?.data as NodeDataType) || currentTaskNodeData;

    let parentTaskDispatcherInfo: {
        componentName: string;
        context: TaskDispatcherDataType;
        task: WorkflowTask;
    } | null = null;

    // Look for parent dispatchers of all types
    for (const taskDispatcherName of TASK_DISPATCHER_NAMES) {
        const taskDispatcherData = nodeData[
            `${taskDispatcherName as keyof typeof TASK_DISPATCHER_CONFIG}Data`
        ] as TaskDispatcherContextType;

        if (!taskDispatcherData) {
            continue;
        }

        const taskDispatcherId: string = (taskDispatcherData as TaskDispatcherDataType)[
            `${taskDispatcherName as keyof typeof TASK_DISPATCHER_CONFIG}Id`
        ];

        const taskDispatcherConfig = TASK_DISPATCHER_CONFIG[taskDispatcherName as keyof typeof TASK_DISPATCHER_CONFIG];

        if (!taskDispatcherId || !taskDispatcherConfig) {
            continue;
        }

        const parentTaskDispatcherTask: WorkflowTask = taskDispatcherConfig.getTask({
            taskDispatcherId,
            tasks: definitionTasks,
        })!;

        if (parentTaskDispatcherTask) {
            parentTaskDispatcherInfo = {
                componentName: taskDispatcherName,
                context: {...taskDispatcherData} as TaskDispatcherDataType,
                task: parentTaskDispatcherTask,
            };

            break;
        }
    }

    // If no parent found, we've reached the root - return current node data
    if (!parentTaskDispatcherInfo) {
        return currentTaskNodeData;
    }

    const parentTaskDispatcherNode = nodesMap.get(parentTaskDispatcherInfo.task.name);

    if (!parentTaskDispatcherNode) {
        return currentTaskNodeData;
    }

    const taskDispatcherConfig =
        TASK_DISPATCHER_CONFIG[parentTaskDispatcherInfo.componentName as keyof typeof TASK_DISPATCHER_CONFIG];

    const subtasks = taskDispatcherConfig.getSubtasks({
        context: {
            ...(parentTaskDispatcherInfo.context as TaskDispatcherContextType),
        },
        task: parentTaskDispatcherInfo.task,
    });

    const taskIndex = subtasks.findIndex((task) => task.name === taskName);

    if (taskIndex >= 0) {
        const updatedSubtasks = [...subtasks];

        const cleanTaskUpdate = {
            ...updatedSubtasks[taskIndex],
            parameters: currentTaskNodeData.parameters,
        };

        updatedSubtasks[taskIndex] = cleanTaskUpdate;

        const updatedParentTask = taskDispatcherConfig.updateTaskParameters({
            context: {...(parentTaskDispatcherInfo.context as TaskDispatcherContextType)},
            task: parentTaskDispatcherInfo.task,
            updatedSubtasks,
        });

        const parentNodeData = parentTaskDispatcherNode.data as NodeDataType;

        const updatedParentNodeData: NodeDataType = {
            ...parentNodeData,
            ...updatedParentTask,
        };

        return getRecursivelyUpdatedRootTaskDispatcherNodeData(updatedParentNodeData, definitionTasks, nodesMap);
    }

    return currentTaskNodeData;
}
