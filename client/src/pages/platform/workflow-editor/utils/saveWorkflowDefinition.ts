import {Workflow, WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {
    BranchCaseType,
    NodeDataType,
    TaskDispatcherContextType,
    UpdateWorkflowMutationType,
    WorkflowDefinitionType,
} from '@/shared/types';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore, {setWorkflowWithoutHistory} from '../stores/useWorkflowDataStore';
import {flattenDefinitionTasks} from './flattenDefinitionTasks';
import getRecursivelyUpdatedTasks from './getRecursivelyUpdatedTasks';
import {getTask} from './getTask';
import insertTaskDispatcherSubtask from './insertTaskDispatcherSubtask';
import stringifyWorkflowDefinition from './stringifyWorkflowDefinition';
import upsertTrigger from './upsertTrigger';
import {isWorkflowMutating, setWorkflowMutating} from './workflowMutationGuard';

interface SaveWorkflowDefinitionProps {
    decorative?: boolean;
    nodeData?: NodeDataType;
    nodeIndex?: number;
    onSuccess?: () => void;
    placeholderId?: string;
    taskDispatcherContext?: TaskDispatcherContextType;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    updatedWorkflowTasks?: Array<WorkflowTask>;
}

export default async function saveWorkflowDefinition({
    decorative,
    nodeData,
    nodeIndex,
    onSuccess,
    placeholderId,
    taskDispatcherContext,
    updateWorkflowMutation,
    updatedWorkflowTasks,
}: SaveWorkflowDefinitionProps) {
    const {workflow} = useWorkflowDataStore.getState();

    let workflowDefinition: WorkflowDefinitionType;

    try {
        workflowDefinition = JSON.parse(workflow.definition!);
    } catch (error) {
        console.error('Failed to parse workflow definition:', error);

        return;
    }

    const workflowTasks: Array<WorkflowTask> = workflow.tasks ?? [];
    const workflowDefinitionTasks: Array<WorkflowTask> = workflowDefinition.tasks ?? [];

    const {
        clusterElements,
        componentName,
        description,
        disabled,
        label,
        maxRetries,
        metadata,
        name,
        operationName,
        parameters,
        taskDispatcher,
        trigger,
        version,
    } = nodeData ?? {};

    let {type} = nodeData ?? {};

    if (trigger) {
        if (!type) {
            type = `${componentName}/v${version}/${operationName}`;
        }

        const newTrigger: WorkflowTrigger = {
            description,
            label,
            metadata,
            name: name!,
            parameters,
            type,
        };

        const existingTriggers: Array<WorkflowTrigger> = workflowDefinition.triggers ?? [];

        executeWorkflowMutation({
            definitionUpdate: {triggers: upsertTrigger(existingTriggers, newTrigger)},
            onSuccess: () => {
                if (onSuccess) {
                    onSuccess();
                }
            },
            updateWorkflowMutation,
            workflow,
            workflowDefinition,
        });

        return;
    }

    if (!type && !trigger) {
        if (taskDispatcher) {
            type = `${componentName}/v${version}`;
        } else {
            type = `${componentName}/v${version}/${operationName}`;
        }
    }

    const newTask: WorkflowTask = {
        clusterElements,
        description,
        disabled,
        label,
        maxRetries,
        metadata,
        name: name!,
        parameters,
        type: type ?? `${componentName}/v${version}/${operationName}`,
    };

    const existingWorkflowTask = workflowTasks?.find((task) => task.name === newTask.name);

    const differenceInCaseCount =
        existingWorkflowTask &&
        componentName === 'branch' &&
        (existingWorkflowTask?.parameters?.cases as BranchCaseType[])?.length !== newTask.parameters?.cases.length;

    const differenceInCaseKeys =
        existingWorkflowTask &&
        componentName === 'branch' &&
        (existingWorkflowTask?.parameters?.cases as BranchCaseType[])?.some((caseItem, index) => {
            const newCaseItem = newTask.parameters?.cases?.[index];

            return caseItem.key !== newCaseItem?.key;
        });

    const differenceInParameters =
        existingWorkflowTask?.parameters &&
        JSON.stringify(existingWorkflowTask.parameters) !== JSON.stringify(newTask.parameters);

    const differenceInType = existingWorkflowTask?.type !== newTask.type;

    const differenceInClusterElements =
        JSON.stringify(existingWorkflowTask?.clusterElements) !== JSON.stringify(newTask.clusterElements);

    if (
        existingWorkflowTask &&
        !decorative &&
        !operationName &&
        !differenceInParameters &&
        !differenceInClusterElements &&
        !differenceInType &&
        !differenceInCaseCount &&
        !differenceInCaseKeys
    ) {
        return;
    }

    let updatedWorkflowDefinitionTasks = workflowDefinitionTasks;

    if (updatedWorkflowTasks) {
        updatedWorkflowDefinitionTasks = updatedWorkflowTasks;
    } else {
        if (existingWorkflowTask) {
            const existingTaskIndex = workflowDefinitionTasks?.findIndex(
                (task) => task.name === existingWorkflowTask.name
            );

            let combinedParameters = {
                ...existingWorkflowTask.parameters,
                ...newTask.parameters,
            };

            if (existingWorkflowTask.type !== newTask.type) {
                combinedParameters = newTask.parameters ?? {};
            }

            const existingDefinitionTask = getTask({
                tasks: workflowDefinitionTasks,
                workflowNodeName: newTask.name,
            });

            const taskToUpdate = existingWorkflowTask.clusterRoot
                ? {
                      ...newTask,
                      clusterElements: {
                          ...(newTask.clusterElements ?? existingDefinitionTask?.clusterElements ?? {}),
                      },
                  }
                : {
                      ...newTask,
                      parameters: combinedParameters,
                  };

            if (existingTaskIndex !== undefined && existingTaskIndex !== -1) {
                if (existingWorkflowTask.type !== newTask.type) {
                    delete updatedWorkflowDefinitionTasks[existingTaskIndex].parameters;
                }

                updatedWorkflowDefinitionTasks = [
                    ...updatedWorkflowDefinitionTasks.slice(0, existingTaskIndex),
                    taskToUpdate,
                    ...updatedWorkflowDefinitionTasks.slice(existingTaskIndex + 1),
                ];
            } else {
                const nestedTask = getTask({
                    tasks: workflowDefinitionTasks,
                    workflowNodeName: existingWorkflowTask.name,
                });

                if (!nestedTask) {
                    console.error(`Task ${existingWorkflowTask.name} not found in workflow definition`);

                    return;
                }

                updatedWorkflowDefinitionTasks = getRecursivelyUpdatedTasks(
                    updatedWorkflowDefinitionTasks,
                    taskToUpdate
                );
            }
        } else {
            updatedWorkflowDefinitionTasks = [...(workflowDefinitionTasks || [])];

            // Reached SYNCHRONOUSLY from the component popover's click handler, and the graph
            // canvas depends on that: `insertTaskDispatcherSubtask` consumes the pending graph
            // connection that the popover's own close handler clears immediately afterwards. An
            // `await` added anywhere above this line would let the close win the race, silently
            // turning "drop a transition on empty space" back into an unconnected node at a free
            // spot — with no test failing, since the insertion tests call that function directly.
            if (taskDispatcherContext?.taskDispatcherId) {
                updatedWorkflowDefinitionTasks = insertTaskDispatcherSubtask({
                    newTask,
                    placeholderId,
                    taskDispatcherContext,
                    tasks: updatedWorkflowDefinitionTasks,
                });
            } else if (nodeIndex !== undefined && nodeIndex > -1) {
                updatedWorkflowDefinitionTasks = [...updatedWorkflowDefinitionTasks];

                updatedWorkflowDefinitionTasks.splice(nodeIndex, 0, newTask);

                // Clear main-axis of saved positions for tasks after the insertion
                // point so dagre can shift them, but preserve cross-axis customization.
                const direction = useLayoutDirectionStore.getState().layoutDirection;
                const mainAxis = direction === 'TB' ? 'y' : 'x';
                const crossAxis = direction === 'TB' ? 'x' : 'y';

                for (let taskIndex = nodeIndex + 1; taskIndex < updatedWorkflowDefinitionTasks.length; taskIndex++) {
                    const task = updatedWorkflowDefinitionTasks[taskIndex];

                    if (task.metadata?.ui?.nodePosition) {
                        const savedCrossValue = task.metadata.ui.nodePosition[crossAxis];

                        updatedWorkflowDefinitionTasks[taskIndex] = {
                            ...task,
                            metadata: {
                                ...task.metadata,
                                ui: {
                                    ...task.metadata.ui,
                                    nodePosition: {
                                        [crossAxis]: savedCrossValue,
                                        [mainAxis]: undefined,
                                    } as {x: number; y: number},
                                },
                            },
                        };
                    }
                }
            } else {
                updatedWorkflowDefinitionTasks.push(newTask);
            }
        }
    }

    executeWorkflowMutation({
        definitionUpdate: {tasks: updatedWorkflowDefinitionTasks},
        newTask: existingWorkflowTask ? undefined : newTask,
        onSuccess,
        updateWorkflowMutation,
        workflow,
        workflowDefinition,
    });
}

interface ExecuteWorkflowMutationProps {
    definitionUpdate: {
        tasks?: Array<WorkflowTask>;
        triggers?: Array<WorkflowTrigger>;
    };
    newTask?: WorkflowTask;
    onSuccess?: () => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
    workflowDefinition: WorkflowDefinitionType;
}

function executeWorkflowMutation({
    definitionUpdate,
    newTask,
    onSuccess,
    updateWorkflowMutation,
    workflow,
    workflowDefinition,
}: ExecuteWorkflowMutationProps) {
    // A second edit arriving while the first is still in flight is DROPPED — silently, with no UI
    // signal: the panel row the user just deleted simply stays. Rapid repeated edits used to be
    // rare, but the graph surfaces (deleting transition rows, drawing transitions back to back)
    // make them an ordinary gesture, so this is now reachable in normal use.
    //
    // The queue this wants is `saveWorkflowNodesPosition`'s: `setPendingDefinition` on the skip,
    // `drainPendingDefinitionMutation` in `onSettled` (see `workflowMutationGuard`). It is NOT
    // wired here because that queue carries a definition STRING and nothing else, while this path
    // also carries `newTask` (optimistic task hydration, server-computed `clusterRoot`) and an
    // `onSuccess` callback across 23 production call sites — a queued write would silently lose
    // both. Making it safe means teaching the queue to carry them, for every caller at once;
    // queueing only some callers would be worse than the drop, since the two halves would disagree
    // about whether a skipped write is lost.
    if (isWorkflowMutating(workflow.id!)) {
        return;
    }

    const updatedDefinition = stringifyWorkflowDefinition({
        ...workflowDefinition,
        ...definitionUpdate,
    });

    const previousWorkflow = workflow;

    let optimisticTasks =
        newTask && definitionUpdate.tasks ? flattenDefinitionTasks(definitionUpdate.tasks) : undefined;

    // Preserve server-computed properties (clusterRoot) that exist in workflow.tasks
    // but not in the JSON definition. Without this, cluster root nodes (DataStream, AI Agent)
    // temporarily render as regular workflow nodes during the optimistic update.
    if (optimisticTasks && workflow.tasks) {
        const existingTasksByName = new Map(workflow.tasks.map((task) => [task.name, task]));

        optimisticTasks = optimisticTasks.map((task) => {
            const existingTask = existingTasksByName.get(task.name);

            if (existingTask?.clusterRoot) {
                return {...task, clusterRoot: existingTask.clusterRoot};
            }

            return task;
        });
    }

    useWorkflowDataStore.getState().setWorkflow({
        ...workflow,
        definition: updatedDefinition,
        ...(optimisticTasks ? {tasks: optimisticTasks} : {}),
    });

    setWorkflowMutating(workflow.id!, true);

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: updatedDefinition,
                version: workflow.version,
            },
        },
        {
            onError: (error) => {
                console.error('Failed to save workflow definition:', error);

                setWorkflowWithoutHistory(previousWorkflow);
            },
            onSettled: () => {
                setWorkflowMutating(workflow.id!, false);
            },
            onSuccess: (updatedWorkflow) => {
                useWorkflowDataStore.getState().setWorkflow({
                    ...updatedWorkflow,
                    definition: updatedDefinition,
                });

                if (onSuccess) {
                    onSuccess();
                }
            },
        }
    );
}
