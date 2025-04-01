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

export const DISPATCHER_TYPE_MAP = {
    condition: {
        contextIdentifier: 'conditionId',
        dataKey: 'conditionData',
    },
    loop: {
        contextIdentifier: 'loopId',
        dataKey: 'loopData',
    },
} as const;

export function buildGenericNodeData(
    baseNodeData: NodeDataType,
    taskDispatcherContext: TaskDispatcherContextType,
    taskDispatcherId: string,
    dispatcherType: keyof typeof DISPATCHER_TYPE_MAP
): NodeDataType {
    const {contextIdentifier} = DISPATCHER_TYPE_MAP[dispatcherType];

    const newNodeData = {
        ...baseNodeData,
        [contextIdentifier]: taskDispatcherId,
    };

    for (const [type, config] of Object.entries(DISPATCHER_TYPE_MAP)) {
        const contextId = taskDispatcherContext[config.contextIdentifier as 'conditionId' | 'loopId'];

        if (contextId) {
            if (type === 'condition') {
                newNodeData[config.dataKey] = {
                    [config.contextIdentifier]: contextId,
                    conditionCase: taskDispatcherContext.conditionCase || CONDITION_CASE_TRUE,
                    conditionId: contextId,
                    index: taskDispatcherContext.index ?? 0,
                };
            } else if (type === 'loop') {
                newNodeData[config.dataKey] = {
                    index: taskDispatcherContext.index ?? 0,
                    loopId: contextId,
                };
            }

            newNodeData.taskDispatcherId = contextId;

            break;
        }
    }

    return newNodeData;
}

export const TASK_DISPATCHER_CONFIG = {
    branch: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType =>
            buildGenericNodeData(baseNodeData, taskDispatcherContext, taskDispatcherId, 'branch'),
        getDispatcherId: (context: TaskDispatcherContextType) => context.branchId,
        getInitialParameters: (properties: Array<PropertyAllType>) => ({
            ...getParametersWithDefaultValues({properties}),
        }),
        initializeParameters: () => ({
            cases: [],
            default: [],
        }),
    },
    condition: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType =>
            buildGenericNodeData(baseNodeData, taskDispatcherContext, taskDispatcherId, 'condition'),
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
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType =>
            buildGenericNodeData(baseNodeData, taskDispatcherContext, taskDispatcherId, 'loop'),
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
