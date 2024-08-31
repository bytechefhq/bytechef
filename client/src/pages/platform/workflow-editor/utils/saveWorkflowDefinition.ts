import {
    ComponentDefinitionApi,
    TaskDispatcherDefinitionApi,
    WorkflowConnectionModel,
    WorkflowModel,
    WorkflowTaskModel,
    WorkflowTriggerModel,
} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {TaskDispatcherKeys} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {WorkflowDefinitionType} from '@/shared/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

const SPACE = 4;

type UpdateWorkflowRequestType = {
    id: string;
    workflowModel: WorkflowModel;
};

type NodeDataType = {
    componentName: string;
    connections?: Array<WorkflowConnectionModel>;
    description?: string;
    icon?: JSX.Element | string;
    label?: string;
    metadata?: {
        ui?: {
            dynamicPropertyTypes?: {[key: string]: string};
        };
    };
    name: string;
    operationName?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: {[key: string]: any};
    taskDispatcher?: boolean;
    trigger?: boolean;
    type?: string;
    version?: number;
};

export default async function saveWorkflowDefinition(
    nodeData: NodeDataType,
    workflow: WorkflowModel,
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequestType, unknown>,
    index?: number,
    onSuccess?: (workflow: WorkflowModel) => void
) {
    const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

    if (nodeData.trigger) {
        const newTrigger: WorkflowTriggerModel = {
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
                workflowModel: {
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

    const queryClient = new QueryClient();

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

    const newTask: WorkflowTaskModel = {
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

    let tasks: WorkflowTaskModel[];

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

        const combinedTask: WorkflowTaskModel = {
            ...existingWorkflowTask,
            ...newTask,
            parameters: combinedParameters,
        };

        tasks[existingTaskIndex] = combinedTask;
    } else if (index !== undefined && index > -1) {
        tasks = [...(workflowDefinition.tasks || [])];

        tasks.splice(index, 0, newTask);
    } else {
        tasks = [...(workflowDefinition.tasks || []), newTask];
    }

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflowModel: {
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
