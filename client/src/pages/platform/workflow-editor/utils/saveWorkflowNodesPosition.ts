import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType, UpdateWorkflowMutationType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {isWorkflowMutating, setWorkflowMutating} from './workflowMutationGuard';

const SPACE = 4;

interface SaveWorkflowNodesPositionProps {
    clearPositionNodeIds?: Set<string>;
    draggedNodeId: string;
    invalidateWorkflowQueries: () => void;
    nodePositions: Record<string, {x: number; y: number}>;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export function updateTaskPositions(
    tasks: WorkflowTask[],
    nodePositions: Record<string, {x: number; y: number}>,
    clearPositionNodeIds?: Set<string>
): WorkflowTask[] {
    if (!Array.isArray(tasks)) {
        return tasks;
    }

    return tasks.map((task) => {
        let updatedTask = {...task};
        const position = nodePositions[task.name];

        if (position) {
            updatedTask.metadata = {
                ...task.metadata,
                ui: {
                    ...task.metadata?.ui,
                    nodePosition: position,
                },
            };
        } else if (clearPositionNodeIds?.has(task.name) && task.metadata?.ui?.nodePosition) {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const {nodePosition, ...remainingUi} = task.metadata.ui;

            updatedTask.metadata = {
                ...task.metadata,
                ui: remainingUi,
            };
        }

        if (updatedTask.parameters?.caseTrue) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    caseTrue: updateTaskPositions(
                        updatedTask.parameters.caseTrue as WorkflowTask[],
                        nodePositions,
                        clearPositionNodeIds
                    ),
                },
            };
        }

        if (updatedTask.parameters?.caseFalse) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    caseFalse: updateTaskPositions(
                        updatedTask.parameters.caseFalse as WorkflowTask[],
                        nodePositions,
                        clearPositionNodeIds
                    ),
                },
            };
        }

        if (updatedTask.parameters?.iteratee) {
            if (Array.isArray(updatedTask.parameters.iteratee)) {
                updatedTask = {
                    ...updatedTask,
                    parameters: {
                        ...updatedTask.parameters,
                        iteratee: updateTaskPositions(
                            updatedTask.parameters.iteratee as WorkflowTask[],
                            nodePositions,
                            clearPositionNodeIds
                        ),
                    },
                };
            }
        }

        if (updatedTask.parameters?.cases) {
            const updatedCases = (updatedTask.parameters.cases as BranchCaseType[]).map((caseItem) => ({
                ...caseItem,
                tasks: caseItem.tasks
                    ? updateTaskPositions(caseItem.tasks, nodePositions, clearPositionNodeIds)
                    : caseItem.tasks,
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
                        default: updateTaskPositions(
                            updatedTask.parameters?.default as WorkflowTask[],
                            nodePositions,
                            clearPositionNodeIds
                        ),
                    },
                };
            }
        }

        if (updatedTask.parameters?.tasks) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    tasks: updateTaskPositions(
                        updatedTask.parameters.tasks as WorkflowTask[],
                        nodePositions,
                        clearPositionNodeIds
                    ),
                },
            };
        }

        if (updatedTask.parameters?.branches) {
            const updatedBranches = (updatedTask.parameters.branches as WorkflowTask[][]).map((branch) =>
                updateTaskPositions(branch, nodePositions, clearPositionNodeIds)
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

export default function saveWorkflowNodesPosition({
    clearPositionNodeIds,
    draggedNodeId,
    invalidateWorkflowQueries,
    nodePositions,
    updateWorkflowMutation,
}: SaveWorkflowNodesPositionProps) {
    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        return;
    }

    const workflowDefinition = JSON.parse(workflow.definition);

    // Update trigger position if it was the dragged node
    if (workflowDefinition.triggers?.[0]?.name === draggedNodeId) {
        const triggerPosition = nodePositions[draggedNodeId];

        if (triggerPosition) {
            workflowDefinition.triggers[0] = {
                ...workflowDefinition.triggers[0],
                metadata: {
                    ...workflowDefinition.triggers[0].metadata,
                    ui: {
                        ...workflowDefinition.triggers[0].metadata?.ui,
                        nodePosition: triggerPosition,
                    },
                },
            };
        }
    }

    // Update task positions recursively
    if (workflowDefinition.tasks) {
        workflowDefinition.tasks = updateTaskPositions(workflowDefinition.tasks, nodePositions, clearPositionNodeIds);
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
            },
        }
    );
}
