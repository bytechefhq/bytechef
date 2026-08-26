import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType, TaskDispatcherContextType, TaskDispatcherDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';

function getTaskDispatcherPropertyName(taskDispatcherName: string): string {
    switch (taskDispatcherName) {
        case 'fork-join':
            return 'forkJoin';
        case 'on-error':
            return 'onError';
        case 'terminate':
            return 'terminate';
        default:
            return taskDispatcherName;
    }
}

/**
 * Walks a subtask up through every enclosing task dispatcher, rewriting each ancestor's
 * subtasks with the edited descendant, until it reaches the root task in the workflow
 * definition. Shared by every edge-rendered chip that mutates a task dispatcher's own
 * structural parameters (branch case keys) from outside the properties panel — those chips
 * can't rely on `useWorkflowNodeDetailsPanelStore`'s `currentNode` (a different node, or none,
 * may be selected while an edge chip is edited), so the update must be threaded from the edited
 * node all the way back to the root before calling `saveWorkflowDefinition`.
 */
export default function getRecursivelyUpdatedRootTaskDispatcherNodeData(
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

    for (const taskDispatcherName of TASK_DISPATCHER_NAMES) {
        const propertyName = getTaskDispatcherPropertyName(taskDispatcherName);

        const taskDispatcherData = nodeData[
            `${propertyName}Data` as keyof typeof nodeData
        ] as TaskDispatcherContextType;

        if (!taskDispatcherData) {
            continue;
        }

        const taskDispatcherId: string = (taskDispatcherData as unknown as Record<string, string>)[`${propertyName}Id`];

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
