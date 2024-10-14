import {
    ComponentDefinitionApi,
    TaskDispatcherDefinitionApi,
    Workflow,
    WorkflowTask,
    WorkflowTrigger,
} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {TaskDispatcherKeys} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {NodeDataType, WorkflowDefinitionType} from '@/shared/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

import getNextPlaceholderId from './getNextPlaceholderId';

const SPACE = 4;

type UpdateWorkflowRequestType = {
    id: string;
    workflow: Workflow;
};

export default async function saveWorkflowDefinition(
    nodeData: NodeDataType,
    workflow: Workflow,
    updateWorkflowMutation: UseMutationResult<Workflow, Error, UpdateWorkflowRequestType, unknown>,
    queryClient: QueryClient,
    nodeIndex?: number,
    onSuccess?: (workflow: Workflow) => void
) {
    const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

    if (nodeData.trigger) {
        const newTrigger: WorkflowTrigger = {
            connections: nodeData.connections,
            description: nodeData.description,
            label: nodeData.label,
            name: nodeData.name,
            parameters: nodeData.parameters,
            type: nodeData.type ?? `${nodeData.componentName}/v${nodeData.version}/${nodeData.operationName}`,
        };

        updateWorkflowMutation.mutate(
            {
                id: workflow.id!,
                workflow: {
                    definition: JSON.stringify(
                        {
                            ...workflowDefinition,
                            triggers: [newTrigger],
                        },
                        null,
                        SPACE
                    ),
                    version: workflow.version,
                },
            },
            {
                onSuccess,
            }
        );

        return;
    }

    const {componentName, description, label, metadata, name, parameters, taskDispatcher, type} = nodeData;

    let {version} = nodeData;

    let {operationName} = nodeData;

    if (taskDispatcher && componentName && version) {
        const newNodeTaskDispatcherDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new TaskDispatcherDefinitionApi().getTaskDispatcherDefinition({
                    taskDispatcherName: componentName,
                    taskDispatcherVersion: version!,
                }),
            queryKey: TaskDispatcherKeys.taskDispatcherDefinition({
                taskDispatcherName: componentName,
                taskDispatcherVersion: version,
            }),
        });

        if (!newNodeTaskDispatcherDefinition) {
            return;
        }
    }

    if (!operationName && !taskDispatcher) {
        const newNodeComponentDefinition = await queryClient.fetchQuery({
            queryFn: () => new ComponentDefinitionApi().getComponentDefinition({componentName}),
            queryKey: ComponentDefinitionKeys.componentDefinition({componentName}),
        });

        if (!version) {
            version = newNodeComponentDefinition?.version;
        }

        if (!newNodeComponentDefinition) {
            return;
        }

        operationName = newNodeComponentDefinition.actions?.[0].name;
    }

    const newTask: WorkflowTask = {
        description,
        label,
        metadata,
        name,
        parameters,
        type: type ? type : `${componentName}/v${version}/${operationName}`,
    };

    const existingWorkflowTask = workflowDefinition.tasks?.find((task) => task.name === newTask.name);

    if (
        existingWorkflowTask &&
        (!operationName ||
            (existingWorkflowTask.parameters &&
                JSON.stringify(existingWorkflowTask.parameters) === JSON.stringify(newTask.parameters))) &&
        existingWorkflowTask.type === newTask.type
    ) {
        return;
    }

    let tasks: WorkflowTask[];

    if (existingWorkflowTask) {
        const existingTaskIndex = workflowDefinition.tasks?.findIndex(
            (task) => task.name === existingWorkflowTask.name
        );

        if (existingTaskIndex === undefined) {
            return;
        }

        tasks = [...(workflowDefinition.tasks || [])];

        if (existingWorkflowTask.type !== newTask.type) {
            delete tasks[existingTaskIndex].parameters;
        }

        const combinedParameters = {
            ...existingWorkflowTask.parameters,
            ...newTask.parameters,
        };

        const combinedTask: WorkflowTask = {
            ...existingWorkflowTask,
            ...newTask,
            parameters: combinedParameters,
        };

        tasks[existingTaskIndex] = combinedTask;
    } else {
        tasks = [...(workflowDefinition.tasks || [])];

        if (nodeData.metadata?.ui?.condition) {
            if (nodeIndex !== undefined && nodeIndex !== -1) {
                const tasksAfterCurrent = tasks.slice(nodeIndex);

                const placeholderCorrectedTasksAfterCurrent = tasksAfterCurrent.map((task) => {
                    let taskCondition = task.metadata?.ui?.condition;

                    if (taskCondition && nodeData.metadata?.ui?.condition) {
                        const taskConditionSide = taskCondition.includes('left')
                            ? 'left'
                            : taskCondition.includes('right')
                              ? 'right'
                              : null;

                        const nodeConditionSide = nodeData.metadata.ui.condition.includes('left')
                            ? 'left'
                            : nodeData.metadata.ui.condition.includes('right')
                              ? 'right'
                              : null;

                        if (taskConditionSide && nodeConditionSide && taskConditionSide === nodeConditionSide) {
                            taskCondition = getNextPlaceholderId(taskCondition);
                        }
                    }

                    if (task.metadata?.ui?.condition) {
                        return {
                            ...task,
                            metadata: {
                                ...task.metadata,
                                ui: {
                                    ...task.metadata?.ui,
                                    // condition: getNextPlaceholderId(task.metadata?.ui?.condition),
                                    condition: taskCondition,
                                },
                            },
                        };
                    }

                    return task;
                });

                tasks = [...tasks.slice(0, nodeIndex), ...placeholderCorrectedTasksAfterCurrent];

                tasks.splice(nodeIndex, 0, newTask);
            } else {
                tasks.push(newTask);
            }
        } else if (nodeIndex !== undefined && nodeIndex > -1) {
            const tasksAfterCurrent = tasks.slice(nodeIndex);

            tasks = [...tasks.slice(0, nodeIndex), ...tasksAfterCurrent];

            tasks.splice(nodeIndex, 0, newTask);
        } else {
            tasks.push(newTask);
        }
    }

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(
                    {
                        ...workflowDefinition,
                        tasks,
                    },
                    null,
                    SPACE
                ),
                version: workflow.version,
            },
        },
        {
            onSuccess,
        }
    );
}
