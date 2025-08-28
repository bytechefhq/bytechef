import {
    CONDITION_CASE_FALSE,
    CONDITION_CASE_TRUE,
    TASK_DISPATCHER_NAMES,
    TASK_DISPATCHER_SUBTASK_COLLECTIONS,
} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {
    BranchCaseType,
    BuildNodeDataType,
    NodeDataType,
    PropertyAllType,
    TaskDispatcherContextType,
    UpdateTaskParametersType,
} from '@/shared/types';
import {Node} from '@xyflow/react';

import getParametersWithDefaultValues from './getParametersWithDefaultValues';

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
            taskDispatcherContext[config.contextIdentifier as 'branchId' | 'conditionId' | 'eachId' | 'loopId'];

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
            } else if (type === 'loopBreak') {
                newNodeData.loopBreakData = {
                    loopBreakId: taskDispatcherId,
                };
            } else if (type === 'branch') {
                newNodeData.branchData = {
                    branchId: taskDispatcherId,
                    caseKey: taskDispatcherContext.caseKey ?? 'default',
                    index: taskDispatcherContext.index ?? 0,
                };
            } else if (type === 'parallel') {
                newNodeData.parallelData = {
                    index: taskDispatcherContext.index ?? 0,
                    parallelId: taskDispatcherId,
                };
            } else if (type === 'each') {
                newNodeData.eachData = {
                    eachId: taskDispatcherId,
                    index: 0,
                };
            } else if (type === 'fork-join') {
                newNodeData.forkJoinData = {
                    branchIndex: taskDispatcherContext.branchIndex ?? 0,
                    forkJoinId: taskDispatcherId,
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
            const caseKeyRaw = parts[2];

            let caseKey: string | number = caseKeyRaw;

            const isCaseKeyNumeric = /^-?\d*\.?\d+$/.test(caseKeyRaw);

            if (isCaseKeyNumeric) {
                caseKey = parseFloat(caseKeyRaw);
            }

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
        getNewCaseKey: (cases: Array<BranchCaseType>): string | number => {
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
        getSubtasks: ({
            context,
            getAllSubtasks = false,
            node,
            task,
        }: {
            context?: TaskDispatcherContextType;
            getAllSubtasks?: boolean;
            node?: Node;
            task?: WorkflowTask;
        }): Array<WorkflowTask> => {
            const parameters = (node?.data as NodeDataType)?.parameters || task?.parameters;

            if (!parameters) {
                return [];
            }

            if (getAllSubtasks) {
                return [
                    ...(parameters?.default || []),
                    ...(parameters?.cases || []).flatMap((caseItem: BranchCaseType) => caseItem.tasks || []),
                ];
            }

            const caseKey = context?.caseKey || 'default';

            if (caseKey === 'default') {
                return parameters?.default || [];
            }

            const cases = [...(parameters?.cases || [])];
            const existingCaseIndex = cases.findIndex((caseItem) => caseItem.key === caseKey);

            if (existingCaseIndex >= 0) {
                return cases[existingCaseIndex].tasks;
            }

            return [];
        },
        getTask: getTaskDispatcherTask,
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
        getSubtasks: ({
            context,
            getAllSubtasks = false,
            node,
            task,
        }: {
            context?: TaskDispatcherContextType;
            getAllSubtasks?: boolean;
            node?: Node;
            task?: WorkflowTask;
        }): Array<WorkflowTask> => {
            const parameters = (node?.data as NodeDataType)?.parameters || task?.parameters;

            if (!parameters) {
                return [];
            }

            if (getAllSubtasks) {
                return [...(parameters?.caseTrue || []), ...(parameters?.caseFalse || [])];
            }

            const conditionCase = (context?.conditionCase as 'caseTrue' | 'caseFalse') || CONDITION_CASE_TRUE;

            return conditionCase === CONDITION_CASE_TRUE ? parameters?.caseTrue || [] : parameters?.caseFalse || [];
        },
        getTask: getTaskDispatcherTask,
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
    each: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType =>
            buildGenericNodeData(baseNodeData, taskDispatcherContext, taskDispatcherId, 'each'),
        contextIdentifier: 'eachId',
        dataKey: 'eachData',
        extractContextFromPlaceholder: (placeholderId: string): TaskDispatcherContextType => {
            const parts = placeholderId.split('-');
            const index = parseInt(parts[parts.length - 1] || '-1');

            return {index, taskDispatcherId: parts[0]};
        },
        getDispatcherId: (context: TaskDispatcherContextType) => context.eachId,
        getInitialParameters: (properties: Array<PropertyAllType>) => ({
            ...getParametersWithDefaultValues({properties}),
        }),
        getSubtasks: ({node, task}: {node?: Node; task?: WorkflowTask}): Array<WorkflowTask> => {
            const parameters = (node?.data as NodeDataType)?.parameters || task?.parameters;

            if (!parameters || !parameters.iteratee) {
                return [];
            }

            return [parameters.iteratee];
        },
        getTask: getTaskDispatcherTask,
        initializeParameters: () => ({
            iteratee: null,
        }),
        updateTaskParameters: ({task, updatedSubtasks}: UpdateTaskParametersType): WorkflowTask => ({
            ...task,
            parameters: {
                ...task.parameters,
                iteratee: updatedSubtasks[0] || null,
            },
        }),
    },
    'fork-join': {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType =>
            buildGenericNodeData(baseNodeData, taskDispatcherContext, taskDispatcherId, 'fork-join'),
        contextIdentifier: 'forkJoinId',
        dataKey: 'forkJoinData',
        extractContextFromPlaceholder: (placeholderId: string): TaskDispatcherContextType => {
            const parts = placeholderId.split('-');
            const index = parseInt(parts[parts.length - 1] || '-1');

            return {branchIndex: index ?? 0, index, taskDispatcherId: parts[0]};
        },
        getDispatcherId: (context: TaskDispatcherContextType) => context.forkJoinId,
        getInitialParameters: (properties: Array<PropertyAllType>) => ({
            ...getParametersWithDefaultValues({properties}),
            branches: [],
        }),
        getSubtasks: ({
            context,
            getAllSubtasks,
            task,
        }: {
            context?: TaskDispatcherContextType;
            getAllSubtasks?: boolean;
            task: WorkflowTask;
        }): Array<WorkflowTask> => {
            const branches = task.parameters?.branches || [];

            if (getAllSubtasks) {
                return branches.flat();
            }

            const branchIndex = context?.branchIndex ?? 0;

            if (branchIndex >= 0 && branchIndex < branches.length) {
                return branches[branchIndex] || [];
            }

            return [];
        },
        getTask: getTaskDispatcherTask,
        initializeParameters: () => ({
            branches: [],
        }),
        updateTaskParameters: ({context, task, updatedSubtasks}: UpdateTaskParametersType): WorkflowTask => {
            const branches = [...(task.parameters?.branches || [])];
            const branchIndex = context?.branchIndex ?? 0;

            if (branchIndex >= 0 && branchIndex < branches.length) {
                branches[branchIndex] = updatedSubtasks;
            } else {
                while (branches.length <= branchIndex) {
                    branches.push([]);
                }

                branches[branchIndex] = updatedSubtasks;
            }

            return {
                ...task,
                parameters: {
                    ...task.parameters,
                    branches: branches,
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
        getSubtasks: ({node, task}: {node?: Node; task?: WorkflowTask}): Array<WorkflowTask> => {
            const parameters = (node?.data as NodeDataType)?.parameters || task?.parameters;

            if (!parameters) {
                return [];
            }

            return parameters?.iteratee || [];
        },
        getTask: getTaskDispatcherTask,
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
    loopBreak: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType =>
            buildGenericNodeData(baseNodeData, taskDispatcherContext, taskDispatcherId, 'loopBreak'),

        contextIdentifier: 'loopBreakId',
        dataKey: 'loopBreakData',
        extractContextFromPlaceholder: (placeholderId: string): TaskDispatcherContextType => {
            const parts = placeholderId.split('-');
            const index = parseInt(parts[parts.length - 1] || '-1');

            return {index, taskDispatcherId: parts[0]};
        },
        getDispatcherId: (context: TaskDispatcherContextType) => context.loopBreakId,
        getInitialParameters: (properties: Array<PropertyAllType>) => ({
            ...getParametersWithDefaultValues({properties}),
        }),
        getSubtasks: () => [],
        getTask: getTaskDispatcherTask,
        initializeParameters: () => ({}),
        updateTaskParameters: ({task}: UpdateTaskParametersType): WorkflowTask => task,
    },
    parallel: {
        buildNodeData: ({baseNodeData, taskDispatcherContext, taskDispatcherId}: BuildNodeDataType): NodeDataType =>
            buildGenericNodeData(baseNodeData, taskDispatcherContext, taskDispatcherId, 'parallel'),
        contextIdentifier: 'parallelId',
        dataKey: 'parallelData',
        extractContextFromPlaceholder: (placeholderId: string): TaskDispatcherContextType => {
            const parts = placeholderId.split('-');
            const index = parseInt(parts[parts.length - 1] || '-1');

            return {index, taskDispatcherId: parts[0]};
        },
        getDispatcherId: (context: TaskDispatcherContextType) => context.parallelId,
        getInitialParameters: (properties: Array<PropertyAllType>) => ({
            ...getParametersWithDefaultValues({properties}),
        }),
        getSubtasks: ({node, task}: {node?: Node; task?: WorkflowTask}): Array<WorkflowTask> => {
            const parameters = (node?.data as NodeDataType)?.parameters || task?.parameters;

            if (!parameters) {
                return [];
            }

            return parameters?.tasks || [];
        },
        getTask: getTaskDispatcherTask,
        initializeParameters: () => ({
            tasks: [],
        }),
        updateTaskParameters: ({task, updatedSubtasks}: UpdateTaskParametersType): WorkflowTask => ({
            ...task,
            parameters: {
                ...task.parameters,
                tasks: updatedSubtasks,
            },
        }),
    },
};

/**
 * Find the parent task dispatcher ID for a nested task
 */
export function getParentTaskDispatcherTask(taskId: string, tasks: WorkflowTask[]): WorkflowTask | undefined {
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
                context: {conditionCase: CONDITION_CASE_TRUE, taskDispatcherId: task.name},
                task,
            });

            const falseBranchTasks = config.getSubtasks({
                context: {conditionCase: CONDITION_CASE_FALSE, taskDispatcherId: task.name},
                task,
            });

            return [...trueBranchTasks, ...falseBranchTasks].some((subtask) => subtask.name === taskId);
        }

        const allSubtasks = config.getSubtasks({getAllSubtasks: true, task});

        return allSubtasks.some((subtask) => subtask.name === taskId);
    });
}

const ALL_COLLECTION_NAMES = Object.values(TASK_DISPATCHER_SUBTASK_COLLECTIONS).flat();

type GetParentTaskDispatcherTaskType = {
    taskDispatcherId: string;
    tasks: Array<WorkflowTask>;
};

export function getTaskDispatcherTask({
    taskDispatcherId,
    tasks,
}: GetParentTaskDispatcherTaskType): WorkflowTask | undefined {
    for (const task of tasks) {
        if (task?.name === taskDispatcherId) {
            return task;
        }

        if (task.parameters) {
            for (const collectionName of ALL_COLLECTION_NAMES) {
                let subtasks = task.parameters[collectionName];

                if (collectionName === 'cases') {
                    subtasks = subtasks?.flatMap((branchCase: BranchCaseType) => branchCase.tasks);
                } else if (collectionName === 'branches') {
                    subtasks = subtasks?.flat();
                } else if (collectionName === 'iteratee') {
                    if (subtasks && typeof subtasks === 'object' && !Array.isArray(subtasks)) {
                        subtasks = [subtasks];
                    } else if (!Array.isArray(subtasks)) {
                        subtasks = [];
                    }
                }

                if (Array.isArray(subtasks) && subtasks.length > 0) {
                    const foundTask = getTaskDispatcherTask({taskDispatcherId, tasks: subtasks});

                    if (foundTask) {
                        return foundTask;
                    }
                }
            }
        }
    }

    return undefined;
}
