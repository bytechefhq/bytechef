import {SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType, UpdateWorkflowMutationType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {
    drainPendingDefinitionMutation,
    hasPendingDefinition,
    isWorkflowMutating,
    setPendingDefinition,
    setWorkflowMutating,
} from './workflowMutationGuard';

interface ClearAllNodePositionsProps {
    incrementLayoutResetCounter: () => void;
    invalidateWorkflowQueries: () => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export function clearTaskPositions(tasks: WorkflowTask[]): WorkflowTask[] {
    if (!Array.isArray(tasks)) {
        return tasks;
    }

    return tasks.map((task) => {
        let updatedTask = {...task};

        if (task.metadata?.ui?.nodePosition) {
            updatedTask.metadata = {
                ...task.metadata,
                ui: {
                    ...task.metadata.ui,
                    nodePosition: undefined,
                },
            };
        }

        if (updatedTask.parameters?.caseTrue) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    caseTrue: clearTaskPositions(updatedTask.parameters.caseTrue as WorkflowTask[]),
                },
            };
        }

        if (updatedTask.parameters?.caseFalse) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    caseFalse: clearTaskPositions(updatedTask.parameters.caseFalse as WorkflowTask[]),
                },
            };
        }

        if (updatedTask.parameters?.iteratee) {
            if (Array.isArray(updatedTask.parameters.iteratee)) {
                updatedTask = {
                    ...updatedTask,
                    parameters: {
                        ...updatedTask.parameters,
                        iteratee: clearTaskPositions(updatedTask.parameters.iteratee as WorkflowTask[]),
                    },
                };
            } else if (
                typeof updatedTask.parameters.iteratee === 'object' &&
                (updatedTask.parameters.iteratee as WorkflowTask).name
            ) {
                const [updatedIteratee] = clearTaskPositions([updatedTask.parameters.iteratee as WorkflowTask]);

                updatedTask = {
                    ...updatedTask,
                    parameters: {
                        ...updatedTask.parameters,
                        iteratee: updatedIteratee,
                    },
                };
            }
        }

        if (updatedTask.parameters?.cases) {
            const updatedCases = (updatedTask.parameters.cases as BranchCaseType[]).map((caseItem) => ({
                ...caseItem,
                tasks: caseItem.tasks ? clearTaskPositions(caseItem.tasks) : caseItem.tasks,
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
                        default: clearTaskPositions(updatedTask.parameters?.default as WorkflowTask[]),
                    },
                };
            }
        }

        if (updatedTask.parameters?.tasks) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    tasks: clearTaskPositions(updatedTask.parameters.tasks as WorkflowTask[]),
                },
            };
        }

        if (updatedTask.parameters?.branches) {
            const updatedBranches = (updatedTask.parameters.branches as WorkflowTask[][]).map((branch) =>
                clearTaskPositions(branch)
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

export default function clearAllNodePositions({
    incrementLayoutResetCounter,
    invalidateWorkflowQueries,
    updateWorkflowMutation,
}: ClearAllNodePositionsProps) {
    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        return;
    }

    const workflowDefinition = JSON.parse(workflow.definition);

    // Clear trigger position
    if (workflowDefinition.triggers?.[0]?.metadata?.ui?.nodePosition) {
        workflowDefinition.triggers[0] = {
            ...workflowDefinition.triggers[0],
            metadata: {
                ...workflowDefinition.triggers[0].metadata,
                ui: {
                    ...workflowDefinition.triggers[0].metadata.ui,
                    nodePosition: undefined,
                },
            },
        };
    }

    // Clear all task positions recursively
    if (workflowDefinition.tasks) {
        workflowDefinition.tasks = clearTaskPositions(workflowDefinition.tasks);
    }

    // Update the store's definition immediately so the layout effect
    // reads cleared positions when incrementLayoutResetCounter fires.
    const updatedDefinitionStr = JSON.stringify(workflowDefinition, null, SPACE);

    useWorkflowDataStore.setState((state) => ({
        workflow: {
            ...state.workflow,
            definition: updatedDefinitionStr,
        },
    }));

    if (isWorkflowMutating(workflow.id!)) {
        setPendingDefinition(workflow.id!, updatedDefinitionStr);

        return;
    }

    setWorkflowMutating(workflow.id!, true);

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: updatedDefinitionStr,
                version: workflow.version,
            },
        },
        {
            onError: () => {
                invalidateWorkflowQueries();
            },
            onSettled: () => {
                setWorkflowMutating(workflow.id!, false);

                drainPendingDefinitionMutation({
                    incrementLayoutResetCounter,
                    invalidateWorkflowQueries,
                    onError: invalidateWorkflowQueries,
                    updateWorkflowMutation,
                    workflowId: workflow.id!,
                });
            },
            onSuccess: (updatedWorkflow) => {
                const currentWorkflow = useWorkflowDataStore.getState().workflow;

                useWorkflowDataStore.getState().setWorkflow({
                    ...currentWorkflow,
                    version: updatedWorkflow.version,
                });

                if (!hasPendingDefinition(workflow.id!)) {
                    invalidateWorkflowQueries();
                    incrementLayoutResetCounter();
                }
            },
        }
    );
}
