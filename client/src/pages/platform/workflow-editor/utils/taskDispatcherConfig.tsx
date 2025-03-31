import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {
    BuildNodeDataType,
    NodeDataType,
    PropertyAllType,
    TaskDispatcherContextType,
    UpdateTaskParametersType,
} from '@/shared/types';

import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import getParentTaskDispatcherTask from './getParentTaskDispatcherTask';

export const TASK_DISPATCHER_CONFIG = {
    condition: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType => {
            const newNodeData = {
                ...baseNodeData,
                conditionId: taskDispatcherId,
            };

            if (taskDispatcherContext?.conditionId) {
                newNodeData.conditionData = {
                    conditionCase: taskDispatcherContext.conditionCase as string,
                    conditionId: taskDispatcherContext.conditionId as string,
                    index: taskDispatcherContext.index as number,
                };

                newNodeData.taskDispatcherId = taskDispatcherContext.conditionId;
            } else if (taskDispatcherContext.loopId) {
                newNodeData.loopData = {
                    index: taskDispatcherContext.index as number,
                    loopId: taskDispatcherContext.loopId as string,
                };

                newNodeData.taskDispatcherId = taskDispatcherContext.loopId;
            }

            return newNodeData;
        },
        extractContextFromPlaceholder: (placeholderId: string): TaskDispatcherContextType => {
            const parts = placeholderId.split('-');
            const index = parseInt(parts[parts.length - 1] || '-1');
            const conditionCase = parts[2] === 'left' ? CONDITION_CASE_TRUE : CONDITION_CASE_FALSE;

            return {
                conditionCase,
                index,
                taskDispatcherId: parts[0],
            };
        },
        getDispatcherId: (context: TaskDispatcherContextType) => context.conditionId,
        getInitialParameters: (properties: Array<PropertyAllType>) => ({
            ...getParametersWithDefaultValues({properties}),
            caseFalse: [],
            caseTrue: [],
        }),
        getParentTask: getParentTaskDispatcherTask,
        getSubtasks: ({
            context,
            task,
        }: {
            context?: TaskDispatcherContextType;
            task: WorkflowTask;
        }): Array<WorkflowTask> => {
            const conditionCase = (context?.conditionCase as 'caseTrue' | 'caseFalse') || CONDITION_CASE_TRUE;
            return conditionCase === CONDITION_CASE_TRUE
                ? task.parameters?.caseTrue || []
                : task.parameters?.caseFalse || [];
        },
        initializeParameters: () => ({
            caseFalse: [],
            caseTrue: [],
        }),
        updateTaskParameters: ({context, task, updatedSubtasks}: UpdateTaskParametersType): WorkflowTask => {
            const conditionCase = context?.conditionCase || CONDITION_CASE_TRUE;

            return {
                ...task,
                parameters: {
                    ...task.parameters,
                    [conditionCase]: updatedSubtasks,
                },
            };
        },
    },
    loop: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType => {
            const newNodeData = {
                ...baseNodeData,
                loopId: taskDispatcherId,
            };

            if (taskDispatcherContext?.conditionId) {
                newNodeData.conditionData = {
                    conditionCase: taskDispatcherContext.conditionCase as string,
                    conditionId: taskDispatcherContext.conditionId as string,
                    index: taskDispatcherContext.index as number,
                };

                newNodeData.taskDispatcherId = taskDispatcherContext.conditionId;
            } else if (taskDispatcherContext?.loopId) {
                newNodeData.loopData = {
                    index: taskDispatcherContext.index as number,
                    loopId: taskDispatcherContext.loopId as string,
                };

                newNodeData.taskDispatcherId = taskDispatcherContext.loopId;
            }

            return newNodeData;
        },
        extractContextFromPlaceholder: (placeholderId: string): TaskDispatcherContextType => {
            const parts = placeholderId.split('-');
            const index = parseInt(parts[parts.length - 1] || '-1');

            return {index, taskDispatcherId: parts[0]};
        },
        getDispatcherId: (context: TaskDispatcherContextType) => context.loopId,
        getInitialParameters: (properties: Array<PropertyAllType>) => ({
            ...getParametersWithDefaultValues({properties}),
        }),
        getParentTask: getParentTaskDispatcherTask,
        getSubtasks: ({task}: {task: WorkflowTask}): Array<WorkflowTask> => task.parameters?.iteratee || [],
        initializeParameters: () => ({
            iteratee: [],
        }),
        updateTaskParameters: ({task, updatedSubtasks}: UpdateTaskParametersType): WorkflowTask => ({
            ...task,
            parameters: {
                ...task.parameters,
                iteratee: updatedSubtasks,
            },
        }),
    },
};

/**
 * Find the parent task dispatcher ID for a nested task
 */
export function findParentTaskDispatcher(taskId: string, tasks: WorkflowTask[]): WorkflowTask | undefined {
    return tasks.find((task) => {
        if (task.name === taskId) {
            return false;
        }

        const componentName = task.type?.split('/')[0];

        if (!componentName || !TASK_DISPATCHER_NAMES.includes(componentName)) {
            return false;
        }

        const config = TASK_DISPATCHER_CONFIG[componentName as keyof typeof TASK_DISPATCHER_CONFIG];

        if (!config) {
            return false;
        }

        if (componentName === 'condition') {
            const trueBranchTasks = config.getSubtasks({
                context: {conditionCase: CONDITION_CASE_TRUE, taskDispatcherId: taskId},
                task,
            });

            const falseBranchTasks = config.getSubtasks({
                context: {conditionCase: CONDITION_CASE_FALSE, taskDispatcherId: taskId},
                task,
            });

            return [...trueBranchTasks, ...falseBranchTasks].some((subtask) => subtask.name === taskId);
        }

        const allSubtasks = config.getSubtasks({task});

        return allSubtasks.some((subtask) => subtask.name === taskId);
    });
}
