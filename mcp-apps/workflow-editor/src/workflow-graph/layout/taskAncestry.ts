// Ported from client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx
// (collectTaskDispatcherData + getTaskAncestry). These two helpers drive the
// orchestration's nesting detection: collectTaskDispatcherData gathers each
// dispatcher's child-task names, and getTaskAncestry resolves which dispatcher
// branch (if any) a given task belongs to.
//
// Adaptation: constants/types imported locally; no behavioral change.

import {
    BranchCaseType,
    BranchChildTasksType,
    ConditionChildTasksType,
    EachChildTasksType,
    ForkJoinChildTasksType,
    LoopChildTasksType,
    MapChildTasksType,
    OnErrorChildTasksType,
    ParallelChildTasksType,
    WorkflowTaskType,
} from '../types';
import {
    CONDITION_CASE_FALSE,
    CONDITION_CASE_TRUE,
    ON_ERROR_ERROR_BRANCH,
    ON_ERROR_MAIN_BRANCH,
    ON_ERROR_WIRE_KEY_ERROR_BRANCH,
    ON_ERROR_WIRE_KEY_MAIN_BRANCH,
    TASK_DISPATCHER_NAMES,
} from './constants';

/**
 * Collects nested tasks for all task dispatchers in the workflow
 */
export function collectTaskDispatcherData(
    task: WorkflowTaskType,
    branchChildTasks: BranchChildTasksType,
    conditionChildTasks: ConditionChildTasksType,
    eachChildTasks: EachChildTasksType,
    forkJoinChildTasks: ForkJoinChildTasksType,
    loopChildTasks: LoopChildTasksType,
    mapChildTasks: MapChildTasksType,
    onErrorChildTasks: OnErrorChildTasksType,
    parallelChildTasks: ParallelChildTasksType
): void {
    const {name, parameters, type} = task;
    const componentName = type.split('/')[0];

    if (!TASK_DISPATCHER_NAMES.includes(componentName)) {
        return;
    }

    if (componentName === 'condition' && parameters) {
        conditionChildTasks[name] = {
            caseFalse: Array.isArray(parameters.caseFalse)
                ? parameters.caseFalse.map((caseFalseSubtask: WorkflowTaskType) => caseFalseSubtask.name)
                : [],
            caseTrue: Array.isArray(parameters.caseTrue)
                ? parameters.caseTrue.map((caseTrueSubtask: WorkflowTaskType) => caseTrueSubtask.name)
                : [],
        };
    } else if (componentName === 'loop' && parameters?.iteratee) {
        loopChildTasks[name] = {
            iteratee: Array.isArray(parameters.iteratee)
                ? parameters.iteratee.map((iteratee: WorkflowTaskType) => iteratee.name)
                : [],
        };
    } else if (componentName === 'map' && parameters?.iteratee) {
        mapChildTasks[name] = {
            iteratee: Array.isArray(parameters.iteratee)
                ? parameters.iteratee.map((iteratee: WorkflowTaskType) => iteratee.name)
                : [],
        };
    } else if (componentName === 'on-error' && parameters) {
        const errorBranch = parameters[ON_ERROR_WIRE_KEY_ERROR_BRANCH];
        const mainBranch = parameters[ON_ERROR_WIRE_KEY_MAIN_BRANCH];

        onErrorChildTasks[name] = {
            mainBranch: Array.isArray(mainBranch) ? mainBranch.map((subtask: WorkflowTaskType) => subtask.name) : [],
            onErrorBranch: Array.isArray(errorBranch)
                ? errorBranch.map((subtask: WorkflowTaskType) => subtask.name)
                : [],
        };
    } else if (componentName === 'branch' && parameters) {
        branchChildTasks[name] = {
            cases: Array.isArray(parameters.cases)
                ? parameters.cases.reduce((accumulator: {[key: string]: string[]}, caseItem: BranchCaseType) => {
                      const caseKey = caseItem.key;

                      const taskNames = Array.isArray(caseItem.tasks)
                          ? caseItem.tasks.map((caseTask: WorkflowTaskType) => caseTask.name)
                          : [];

                      accumulator[caseKey] = taskNames;

                      return accumulator;
                  }, {})
                : {},
            default: Array.isArray(parameters.default)
                ? parameters.default.map((defaultSubtask: WorkflowTaskType) => defaultSubtask.name)
                : [],
        };
    } else if (componentName === 'parallel' && parameters?.tasks) {
        parallelChildTasks[name] = {
            tasks: Array.isArray(parameters.tasks)
                ? parameters.tasks.map((parallelTask: WorkflowTaskType) => parallelTask.name)
                : [],
        };
    } else if (componentName === 'each' && parameters?.iteratee) {
        eachChildTasks[name] = {
            iteratee: parameters.iteratee.name,
        };
    } else if (componentName === 'fork-join') {
        forkJoinChildTasks[name] = {
            branches: Array.isArray(parameters?.branches)
                ? parameters.branches.map((branch: WorkflowTaskType[]) =>
                      Array.isArray(branch) ? branch.map((branchTask: WorkflowTaskType) => branchTask.name) : []
                  )
                : [],
        };
    }
}

type GetTaskAncestryPropsType = {
    branchChildTasks: BranchChildTasksType;
    conditionChildTasks: ConditionChildTasksType;
    eachChildTasks: EachChildTasksType;
    forkJoinChildTasks: ForkJoinChildTasksType;
    loopChildTasks: LoopChildTasksType;
    mapChildTasks: MapChildTasksType;
    onErrorChildTasks: OnErrorChildTasksType;
    parallelChildTasks: ParallelChildTasksType;
    taskName: string;
};

/**
 * Detects if a task is nested inside a task dispatcher and returns relevant nesting data
 */
export function getTaskAncestry({
    branchChildTasks,
    conditionChildTasks,
    eachChildTasks,
    forkJoinChildTasks,
    loopChildTasks,
    mapChildTasks,
    onErrorChildTasks,
    parallelChildTasks,
    taskName,
}: GetTaskAncestryPropsType): {nestingData: Record<string, unknown>; isNested: boolean} {
    let isNested = false;
    let nestingData = {};

    for (const [conditionId, conditionCases] of Object.entries(conditionChildTasks)) {
        const conditionCasesList = [
            {taskNames: conditionCases.caseTrue, value: CONDITION_CASE_TRUE},
            {taskNames: conditionCases.caseFalse, value: CONDITION_CASE_FALSE},
        ];

        const matchingCase = conditionCasesList.find((conditionCase) => conditionCase.taskNames.includes(taskName));

        if (matchingCase) {
            nestingData = {
                conditionData: {
                    conditionCase: matchingCase.value,
                    conditionId,
                    index: matchingCase.taskNames.indexOf(taskName),
                },
            };

            isNested = true;

            break;
        }
    }

    if (!isNested) {
        for (const [onErrorId, onErrorCases] of Object.entries(onErrorChildTasks)) {
            const onErrorCasesList = [
                {taskNames: onErrorCases.mainBranch, value: ON_ERROR_MAIN_BRANCH},
                {taskNames: onErrorCases.onErrorBranch, value: ON_ERROR_ERROR_BRANCH},
            ];

            const matchingOnErrorCase = onErrorCasesList.find((onErrorCase) =>
                onErrorCase.taskNames.includes(taskName)
            );

            if (matchingOnErrorCase) {
                nestingData = {
                    onErrorData: {
                        index: matchingOnErrorCase.taskNames.indexOf(taskName),
                        onErrorCase: matchingOnErrorCase.value,
                        onErrorId,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [loopId, loopData] of Object.entries(loopChildTasks)) {
            if (loopData.iteratee.includes(taskName)) {
                nestingData = {
                    loopData: {
                        index: loopData.iteratee.indexOf(taskName),
                        loopId,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [mapId, mapData] of Object.entries(mapChildTasks)) {
            if (mapData.iteratee.includes(taskName)) {
                nestingData = {
                    mapData: {
                        index: mapData.iteratee.indexOf(taskName),
                        mapId,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [branchId, branchData] of Object.entries(branchChildTasks)) {
            if (branchData.default.includes(taskName)) {
                nestingData = {
                    branchData: {
                        branchId,
                        caseKey: 'default',
                        index: branchData.default.indexOf(taskName),
                    },
                };

                isNested = true;

                break;
            }

            for (const [caseKey, caseTasks] of Object.entries(branchData.cases)) {
                if (caseTasks.includes(taskName)) {
                    nestingData = {
                        branchData: {
                            branchId,
                            caseKey,
                            index: caseTasks.indexOf(taskName),
                        },
                    };

                    isNested = true;

                    break;
                }
            }

            if (isNested) {
                break;
            }
        }
    }

    if (!isNested) {
        for (const [parallelId, parallelData] of Object.entries(parallelChildTasks)) {
            if (parallelData.tasks.includes(taskName)) {
                nestingData = {
                    parallelData: {
                        index: parallelData.tasks.indexOf(taskName),
                        parallelId,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [eachId, eachData] of Object.entries(eachChildTasks)) {
            if (eachData.iteratee === taskName) {
                nestingData = {
                    eachData: {
                        eachId,
                        index: 0,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [forkJoinId, forkJoinData] of Object.entries(forkJoinChildTasks)) {
            const forkJoinSubtaskNameBranches = forkJoinData.branches;

            forkJoinSubtaskNameBranches.forEach((branch, branchIndex) => {
                if (isNested) {
                    return;
                }

                const taskIndex = branch.indexOf(taskName);

                if (taskIndex !== -1) {
                    nestingData = {
                        forkJoinData: {
                            branchIndex,
                            forkJoinId,
                            index: taskIndex,
                        },
                    };

                    isNested = true;
                }
            });

            if (isNested) {
                break;
            }
        }
    }

    return {isNested, nestingData};
}
