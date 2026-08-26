import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType, NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';
import {Node} from '@xyflow/react';

import useWorkflowDataStore, {runWithoutHistory} from '../stores/useWorkflowDataStore';
import stringifyWorkflowDefinition from './stringifyWorkflowDefinition';
import {
    consumePendingDefinition,
    isWorkflowMutating,
    setPendingDefinition,
    setWorkflowMutating,
} from './workflowMutationGuard';

interface SaveWorkflowNodesPositionProps {
    clearPositionNodeIds?: Set<string>;
    draggedNodeId: string;
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
            } else if (
                typeof updatedTask.parameters.iteratee === 'object' &&
                (updatedTask.parameters.iteratee as WorkflowTask).name
            ) {
                // Single-object iteratee (each dispatcher) — wrap, update, unwrap
                const [updatedIteratee] = updateTaskPositions(
                    [updatedTask.parameters.iteratee as WorkflowTask],
                    nodePositions,
                    clearPositionNodeIds
                );

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

        if (updatedTask.parameters?.nodes) {
            updatedTask = {
                ...updatedTask,
                parameters: {
                    ...updatedTask.parameters,
                    nodes: updateTaskPositions(
                        updatedTask.parameters.nodes as WorkflowTask[],
                        nodePositions,
                        clearPositionNodeIds
                    ),
                },
            };
        }

        return updatedTask;
    });
}

export default function saveWorkflowNodesPosition({
    clearPositionNodeIds,
    draggedNodeId,
    nodePositions,
    updateWorkflowMutation,
}: SaveWorkflowNodesPositionProps) {
    const {workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        return;
    }

    let workflowDefinition;

    try {
        workflowDefinition = JSON.parse(workflow.definition);
    } catch (error) {
        console.error('Failed to parse workflow definition:', error);

        return;
    }

    // Update trigger position if a trigger was the dragged node
    if (Array.isArray(workflowDefinition.triggers)) {
        const draggedTriggerIndex = workflowDefinition.triggers.findIndex(
            (trigger: {name: string}) => trigger.name === draggedNodeId
        );

        const triggerPosition = nodePositions[draggedNodeId];

        if (draggedTriggerIndex !== -1 && triggerPosition) {
            const draggedTrigger = workflowDefinition.triggers[draggedTriggerIndex];

            workflowDefinition.triggers[draggedTriggerIndex] = {
                ...draggedTrigger,
                metadata: {
                    ...draggedTrigger.metadata,
                    ui: {
                        ...draggedTrigger.metadata?.ui,
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

    // Update the store's definition immediately so the layout effect
    // can read the latest positions when it re-runs (e.g. on panel toggle).
    // storeTasks uses fingerprint equality that ignores position metadata,
    // so this is the only way to keep positions in sync without a full refetch.
    const updatedDefinitionStr = stringifyWorkflowDefinition(workflowDefinition);

    const updatedTasks = workflow.tasks
        ? updateTaskPositions(workflow.tasks, nodePositions, clearPositionNodeIds)
        : workflow.tasks;

    useWorkflowDataStore.setState((state) => ({
        workflow: {
            ...state.workflow,
            definition: updatedDefinitionStr,
            tasks: updatedTasks,
        },
    }));

    // Optimistically update ReactFlow node data so the pin button appears
    // immediately without waiting for a full re-layout
    const {nodes, setNodes} = useWorkflowDataStore.getState();
    const previousNodes = nodes;

    const previousDefinition = workflow.definition;
    const previousTasks = workflow.tasks;

    const updatedNodes = nodes.map((node) => {
        const position = nodePositions[node.id];

        if (position) {
            return {
                ...node,
                data: {
                    ...node.data,
                    metadata: {
                        ...(node.data as NodeDataType).metadata,
                        ui: {
                            ...(node.data as NodeDataType).metadata?.ui,
                            nodePosition: position,
                        },
                    },
                },
            };
        }

        return node;
    });

    setNodes(updatedNodes);

    if (isWorkflowMutating(workflow.id!)) {
        // Queue the definition so it can be sent when the current mutation settles.
        // Without this, the in-flight mutation's refetch would overwrite the locally
        // synced definition with server data that lacks the skipped positions.
        setPendingDefinition(workflow.id!, updatedDefinitionStr);

        return;
    }

    firePositionMutation({
        definition: updatedDefinitionStr,
        previousDefinition,
        previousNodes,
        previousTasks,
        setNodes,
        updateWorkflowMutation,
        version: workflow.version,
        workflowId: workflow.id!,
    });
}

interface FirePositionMutationProps {
    definition: string;
    previousDefinition: string;
    previousNodes: Node[];
    previousTasks?: WorkflowTask[];
    setNodes: (nodes: Node[]) => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    version?: number;
    workflowId: string;
}

function firePositionMutation({
    definition,
    previousDefinition,
    previousNodes,
    previousTasks,
    setNodes,
    updateWorkflowMutation,
    version,
    workflowId,
}: FirePositionMutationProps) {
    setWorkflowMutating(workflowId, true);

    updateWorkflowMutation.mutate(
        {
            id: workflowId,
            workflow: {
                definition,
                version,
            },
        },
        {
            onError: () => {
                setNodes(previousNodes);

                // Reverting a failed position save must not become an undo step.
                runWithoutHistory(() => {
                    useWorkflowDataStore.setState((state) => ({
                        workflow: {
                            ...state.workflow,
                            definition: previousDefinition,
                            tasks: previousTasks,
                        },
                    }));
                });
            },
            onSettled: () => {
                setWorkflowMutating(workflowId, false);

                // If another position save was skipped while this mutation was
                // in flight, fire it now with the latest queued definition.
                const pendingDefinition = consumePendingDefinition(workflowId);

                if (pendingDefinition) {
                    const currentWorkflow = useWorkflowDataStore.getState().workflow;

                    firePositionMutation({
                        definition: pendingDefinition,
                        previousDefinition: currentWorkflow.definition!,
                        previousNodes: useWorkflowDataStore.getState().nodes,
                        previousTasks: currentWorkflow.tasks,
                        setNodes: useWorkflowDataStore.getState().setNodes,
                        updateWorkflowMutation,
                        version: currentWorkflow.version,
                        workflowId,
                    });
                }
            },
            onSuccess: (updatedWorkflow) => {
                const currentWorkflow = useWorkflowDataStore.getState().workflow;

                useWorkflowDataStore.getState().setWorkflow({
                    ...currentWorkflow,
                    version: updatedWorkflow.version,
                });
            },
        }
    );
}
