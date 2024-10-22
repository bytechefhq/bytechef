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

import getTasksWithConditionChildNode from './getTasksWithConditionChildNode';

const SPACE = 4;

type UpdateWorkflowRequestType = {
    id: string;
    workflow: Workflow;
};

interface SaveWorkflowDefinitionProps {
    nodeData: NodeDataType;
    nodeIndex?: number;
    onSuccess?: (workflow: Workflow) => void;
    queryClient: QueryClient;
    updateWorkflowMutation: UseMutationResult<Workflow, Error, UpdateWorkflowRequestType, unknown>;
    workflow: Workflow;
}

export default async function saveWorkflowDefinition({
    nodeData,
    nodeIndex,
    onSuccess,
    queryClient,
    updateWorkflowMutation,
    workflow,
}: SaveWorkflowDefinitionProps) {
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

    let {operationName, version} = nodeData;

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
        console.log('newTask', newTask);
        console.log('nodeData', nodeData);
        console.log('nodeIndex', nodeIndex);
        console.log('tasks', tasks);

        if (nodeData.metadata?.ui?.condition) {
            // tasks = getTasksWithConditionChildNode({newTask, nodeData, nodeIndex, tasks});
            // tasks = getTasksWithConditionChildNode({newTask, nodeData, nodeIndex, tasks});
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
