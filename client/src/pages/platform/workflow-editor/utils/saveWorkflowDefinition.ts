import {SPACE} from '@/shared/constants';
import {Workflow, WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {
    BranchCaseType,
    NodeDataType,
    TaskDispatcherContextType,
    UpdateWorkflowMutationType,
    WorkflowDefinitionType,
} from '@/shared/types';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {flattenDefinitionTasks} from './flattenDefinitionTasks';
import getRecursivelyUpdatedTasks from './getRecursivelyUpdatedTasks';
import {getTask} from './getTask';
import insertTaskDispatcherSubtask from './insertTaskDispatcherSubtask';
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
            name: name!,
            parameters,
            type,
        };

        executeWorkflowMutation({
            definitionUpdate: {triggers: [newTrigger]},
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

            const taskToUpdate = existingWorkflowTask.clusterRoot
                ? {
                      ...newTask,
                      clusterElements: {
                          ...(newTask.clusterElements || {}),
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
    if (isWorkflowMutating(workflow.id!)) {
        return;
    }

    const updatedDefinition = JSON.stringify(
        {
            ...workflowDefinition,
            ...definitionUpdate,
        },
        null,
        SPACE
    );

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

                useWorkflowDataStore.getState().setWorkflow(previousWorkflow);
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
