import {SPACE} from '@/shared/constants';
import {Workflow, WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {
    BranchCaseType,
    NodeDataType,
    TaskDispatcherContextType,
    UpdateWorkflowMutationType,
    WorkflowDefinitionType,
} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
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

    const workflowDefinition = JSON.parse(workflow.definition!);

    const workflowTasks: Array<WorkflowTask> = workflow.tasks ?? [];
    const workflowDefinitionTasks: Array<WorkflowTask> = workflowDefinition.tasks ?? [];

    const {
        clusterElements,
        componentName,
        description,
        label,
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
                const tasksAfterCurrent = updatedWorkflowDefinitionTasks.slice(nodeIndex);

                updatedWorkflowDefinitionTasks = [
                    ...updatedWorkflowDefinitionTasks.slice(0, nodeIndex),
                    ...tasksAfterCurrent,
                ];

                updatedWorkflowDefinitionTasks.splice(nodeIndex, 0, newTask);
            } else {
                updatedWorkflowDefinitionTasks.push(newTask);
            }
        }
    }

    executeWorkflowMutation({
        definitionUpdate: {tasks: updatedWorkflowDefinitionTasks},
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
    onSuccess?: () => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
    workflowDefinition: WorkflowDefinitionType;
}

function executeWorkflowMutation({
    definitionUpdate,
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

    // Optimistic UI: update definition string immediately so the store reflects the pending change.
    // We do NOT update tasks/triggers optimistically because workflow.tasks is a flattened list
    // (including sub-tasks extracted from task dispatcher parameters) while definitionUpdate.tasks
    // is hierarchical (sub-tasks nested in parameters). Mapping between these structures would
    // drop sub-tasks and break edge computation in the layout.
    const previousWorkflow = workflow;

    useWorkflowDataStore.getState().setWorkflow({
        ...workflow,
        definition: updatedDefinition,
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
            onError: () => {
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
