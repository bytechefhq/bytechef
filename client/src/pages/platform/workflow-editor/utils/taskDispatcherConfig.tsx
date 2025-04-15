import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {
    BranchCaseType,
    BuildNodeDataType,
    NodeDataType,
    PropertyAllType,
    TaskDispatcherContextType,
    UpdateTaskParametersType,
} from '@/shared/types';

import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import getParentTaskDispatcherTask from './getParentTaskDispatcherTask';

export function buildGenericNodeData(
    baseNodeData: NodeDataType,
    taskDispatcherContext: TaskDispatcherContextType,
    taskDispatcherId: string,
    dispatcherType: keyof typeof TASK_DISPATCHER_CONFIG
): NodeDataType {
    const {contextIdentifier} = TASK_DISPATCHER_CONFIG[dispatcherType];

    const newNodeData = {
        ...baseNodeData,
        [contextIdentifier]: taskDispatcherId,
    };

    for (const [type, config] of Object.entries(TASK_DISPATCHER_CONFIG)) {
        const taskDispatcherId =
            taskDispatcherContext[config.contextIdentifier as 'branchId' | 'conditionId' | 'loopId'];

        if (taskDispatcherId) {
            if (type === 'condition') {
                newNodeData.conditionData = {
                    conditionCase: taskDispatcherContext.conditionCase || CONDITION_CASE_TRUE,
                    conditionId: taskDispatcherId,
                    index: taskDispatcherContext.index ?? 0,
                };
            } else if (type === 'loop') {
                newNodeData.loopData = {
                    index: taskDispatcherContext.index ?? 0,
                    loopId: taskDispatcherId,
                };
            } else if (type === 'branch') {
                newNodeData.branchData = {
                    branchId: taskDispatcherId,
                    caseKey: taskDispatcherContext.caseKey ?? 'default',
                    index: taskDispatcherContext.index ?? 0,
                };
            }

            newNodeData.taskDispatcherId = taskDispatcherId;

            break;
        }
    }

    return newNodeData;
}

export const TASK_DISPATCHER_CONFIG = {
    branch: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType =>
            buildGenericNodeData(baseNodeData, taskDispatcherContext, taskDispatcherId, 'branch'),
        contextIdentifier: 'branchId',
        dataKey: 'branchData',
        extractContextFromPlaceholder: (placeholderId: string): TaskDispatcherContextType => {
            const parts = placeholderId.split('-');

            const index = parseInt(parts[parts.length - 1] || '-1');
            const caseKey = parts[2];

            return {
                caseKey,
                index,
                taskDispatcherId: parts[0],
            };
        },
        getDispatcherId: (context: TaskDispatcherContextType) => context.branchId,
        getInitialParameters: (properties: Array<PropertyAllType>) => ({
            ...getParametersWithDefaultValues({properties}),
            cases: [
                {
                    key: 'case_0',
                    tasks: [],
                },
            ],
            default: [],
        }),
        getNewCaseKey: (cases: Array<BranchCaseType>): string => {
            if (!cases) {
                return 'case_0';
            }

            const newCaseKey = `case_${cases.length}`;

            const isNewCaseKeyDuplicate = cases.some((caseItem) => caseItem.key === newCaseKey);

            if (isNewCaseKeyDuplicate) {
                return `${newCaseKey} (1)`;
            }

            const existingCaseIndex = cases.findIndex((caseItem) => caseItem.key === newCaseKey);

            if (existingCaseIndex >= 0) {
                return TASK_DISPATCHER_CONFIG.branch.getNewCaseKey(cases);
            }

            return newCaseKey;
        },
        getParentTask: getParentTaskDispatcherTask,
        getSubtasks: ({
            context,
            getAllSubtasks = false,
            task,
        }: {
            context?: TaskDispatcherContextType;
            getAllSubtasks?: boolean;
            task: WorkflowTask;
        }): Array<WorkflowTask> => {
            if (getAllSubtasks) {
                return [
                    ...(task.parameters?.default || []),
                    ...(task.parameters?.cases || []).flatMap((caseItem: BranchCaseType) => caseItem.tasks || []),
                ];
            }

            const caseKey = context?.caseKey || 'default';

            if (caseKey === 'default') {
                return task.parameters?.default || [];
            }

            const cases = [...(task.parameters?.cases || [])];
            const existingCaseIndex = cases.findIndex((caseItem) => caseItem.key === caseKey);

            if (existingCaseIndex >= 0) {
                return cases[existingCaseIndex].tasks;
            }

            return [];
        },
        initializeParameters: () => ({
            cases: [],
            default: [],
        }),
        updateTaskParameters: ({context, task, updatedSubtasks}: UpdateTaskParametersType): WorkflowTask => {
            const caseKey = context?.caseKey || 'default';

            if (caseKey === 'default') {
                return {
                    ...task,
                    parameters: {
                        ...task.parameters,
                        default: updatedSubtasks,
                    },
                };
            }

            const cases = [...(task.parameters?.cases || [])];

            const existingCaseIndex = cases.findIndex((c) => c.key === caseKey);

            if (existingCaseIndex >= 0) {
                cases[existingCaseIndex] = {
                    ...cases[existingCaseIndex],
                    tasks: updatedSubtasks,
                };
            } else {
                cases.push({
                    key: caseKey,
                    tasks: updatedSubtasks,
                });
            }

            return {
                ...task,
                parameters: {
                    ...task.parameters,
                    cases: cases,
                },
            };
        },
    },
    condition: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType =>
            buildGenericNodeData(baseNodeData, taskDispatcherContext, taskDispatcherId, 'condition'),
        contextIdentifier: 'conditionId',
        dataKey: 'conditionData',
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
            getAllSubtasks = false,
            task,
        }: {
            context?: TaskDispatcherContextType;
            getAllSubtasks?: boolean;
            task: WorkflowTask;
        }): Array<WorkflowTask> => {
            if (getAllSubtasks) {
                return [...(task.parameters?.caseTrue || []), ...(task.parameters?.caseFalse || [])];
            }

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
        contextIdentifier: 'loopId',
        dataKey: 'loopData',
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
