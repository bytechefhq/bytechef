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

import insertNewConditionSubtask from './insertNewConditionSubtask';

const SPACE = 4;

type UpdateWorkflowRequestType = {
    id: string;
    workflow: Workflow;
};

interface SaveWorkflowDefinitionProps {
    conditionId?: string;
    decorative?: boolean;
    nodeData: NodeDataType;
    nodeIndex?: number;
    onSuccess?: () => void;
    placeholderId?: string;
    queryClient: QueryClient;
    subtask?: boolean;
    updateWorkflowMutation: UseMutationResult<void, Error, UpdateWorkflowRequestType, unknown>;
    workflow: Workflow;
}

export default async function saveWorkflowDefinition({
    conditionId,
    decorative,
    nodeData,
    nodeIndex,
    onSuccess,
    placeholderId,
    queryClient,
    subtask,
    updateWorkflowMutation,
    workflow,
}: SaveWorkflowDefinitionProps) {
    const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

    const {componentName, connections, description, label, metadata, name, parameters, taskDispatcher, trigger} =
        nodeData;

    let {operationName, type, version} = nodeData;

    if (trigger) {
        if (!type) {
            type = `${componentName}/v${version}/${operationName}`;
        }

        const newTrigger: WorkflowTrigger = {
            connections,
            description,
            label,
            name,
            parameters,
            type,
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
            queryFn: () =>
                new ComponentDefinitionApi().getComponentDefinition({componentName, componentVersion: version!}),
            queryKey: ComponentDefinitionKeys.componentDefinition({componentName, componentVersion: version!}),
        });

        if (!version) {
            version = newNodeComponentDefinition?.version;
        }

        if (!newNodeComponentDefinition) {
            return;
        }

        operationName = newNodeComponentDefinition.actions?.[0].name;
    }

    if (!type && !trigger) {
        if (taskDispatcher) {
            type = `${componentName}/v${version}`;
        } else {
            type = `${componentName}/v${version}/${operationName}`;
        }
    }

    const newTask: WorkflowTask = {
        description,
        label,
        metadata,
        name,
        parameters,
        type: type ?? `${componentName}/v${version}/${operationName}`,
    };

    const existingWorkflowTask = workflowDefinition.tasks?.find((task) => task.name === newTask.name);

    if (
        existingWorkflowTask &&
        !decorative &&
        !subtask &&
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

        if (conditionId && placeholderId) {
            tasks = insertNewConditionSubtask({conditionId, newTask, placeholderId, tasks});
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
