import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType, UpdateWorkflowMutationType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {clearTaskPositions} from './clearAllNodePositions';
import {isWorkflowMutating, setWorkflowMutating} from './workflowMutationGuard';

const SPACE = 4;

interface RemoveWorkflowNodePositionProps {
    incrementLayoutResetCounter: () => void;
    invalidateWorkflowQueries: () => void;
    nodeName: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

function clearSingleTaskPosition(tasks: WorkflowTask[], nodeName: string): WorkflowTask[] {
    if (!Array.isArray(tasks)) {
        return tasks;
    }

    return tasks.map((task) => {
        let updatedTask = {...task};

        const isMatch = task.name === nodeName;

        if (isMatch && task.metadata?.ui?.nodePosition) {
            updatedTask.metadata = {
                ...task.metadata,
                ui: {
                    ...task.metadata.ui,
                    nodePosition: undefined,
                },
            };
        }

        // For the matched task dispatcher, clear ALL child positions;
        // for non-matching tasks, continue searching by name
        const clearChildren = isMatch
            ? clearTaskPositions
            : (childTasks: WorkflowTask[]) => clearSingleTaskPosition(childTasks, nodeName);

        if (updatedTask.parameters?.caseTrue) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    caseTrue: clearChildren(updatedTask.parameters.caseTrue as WorkflowTask[]),
                },
            };
        }

        if (updatedTask.parameters?.caseFalse) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    caseFalse: clearChildren(updatedTask.parameters.caseFalse as WorkflowTask[]),
                },
            };
        }

        if (updatedTask.parameters?.iteratee) {
            if (Array.isArray(updatedTask.parameters.iteratee)) {
                updatedTask = {
                    ...updatedTask,
                    parameters: {
                        ...updatedTask.parameters,
                        iteratee: clearChildren(updatedTask.parameters.iteratee as WorkflowTask[]),
                    },
                };
            }
        }

        if (updatedTask.parameters?.cases) {
            const updatedCases = (updatedTask.parameters.cases as BranchCaseType[]).map((caseItem) => ({
                ...caseItem,
                tasks: caseItem.tasks ? clearChildren(caseItem.tasks) : caseItem.tasks,
            }));

            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    cases: updatedCases,
                },
            };

            if (updatedTask.parameters?.default) {
                updatedTask = {
                    ...updatedTask,
                    parameters: {
                        ...updatedTask.parameters,
                        default: clearChildren(updatedTask.parameters?.default as WorkflowTask[]),
                    },
                };
            }
        }

        if (updatedTask.parameters?.tasks) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    tasks: clearChildren(updatedTask.parameters.tasks as WorkflowTask[]),
                },
            };
        }

        if (updatedTask.parameters?.branches) {
            const updatedBranches = (updatedTask.parameters.branches as WorkflowTask[][]).map((branch) =>
                clearChildren(branch)
            );

            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    branches: updatedBranches,
                },
            };
        }

        return updatedTask;
    });
}

export default function removeWorkflowNodePosition({
    incrementLayoutResetCounter,
    invalidateWorkflowQueries,
    nodeName,
    updateWorkflowMutation,
}: RemoveWorkflowNodePositionProps) {
    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        return;
    }

    const workflowDefinition = JSON.parse(workflow.definition);

    if (workflowDefinition.triggers?.[0]?.name === nodeName) {
        workflowDefinition.triggers[0] = {
            ...workflowDefinition.triggers[0],
            metadata: {
                ...workflowDefinition.triggers[0].metadata,
                ui: {
                    ...workflowDefinition.triggers[0].metadata?.ui,
                    nodePosition: undefined,
                },
            },
        };
    }

    if (workflowDefinition.tasks) {
        workflowDefinition.tasks = clearSingleTaskPosition(workflowDefinition.tasks, nodeName);
    }

    if (isWorkflowMutating()) {
        return;
    }

    setWorkflowMutating(true);

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(workflowDefinition, null, SPACE),
                version: workflow.version,
            },
        },
        {
            onSettled: () => {
                setWorkflowMutating(false);
            },
            onSuccess: () => {
                invalidateWorkflowQueries();
                incrementLayoutResetCounter();
            },
        }
    );
}
