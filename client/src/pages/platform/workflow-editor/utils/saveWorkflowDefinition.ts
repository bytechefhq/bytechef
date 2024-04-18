import {ComponentDefinitionApi, WorkflowModel, WorkflowTaskModel} from '@/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/queries/platform/componentDefinitions.queries';
import {WorkflowDefinitionType} from '@/types/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

const SPACE = 4;

type UpdateWorkflowRequestType = {
    id: string;
    workflowModel: WorkflowModel;
};

type NodeDataType = {
    actionName?: string;
    componentName: string;
    icon?: JSX.Element | string;
    label?: string;
    name: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: {[key: string]: any};
    type?: string;
};

export default async function saveWorkflowDefinition(
    nodeData: NodeDataType,
    workflow: WorkflowModel,
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequestType, unknown>,
    index?: number,
    onSuccess?: (workflow: WorkflowModel) => void
) {
    const {actionName: currentActionName, componentName, label, name, parameters} = nodeData;

    const queryClient = new QueryClient();

    let actionName = currentActionName;

    if (!actionName) {
        const newNodeComponentDefinition = await queryClient.fetchQuery({
            queryFn: () => new ComponentDefinitionApi().getComponentDefinition({componentName}),
            queryKey: ComponentDefinitionKeys.componentDefinition({componentName}),
        });

        if (!newNodeComponentDefinition) {
            return;
        }

        actionName = newNodeComponentDefinition.actions?.[0].name;
    }

    const newTask: WorkflowTaskModel = {
        label,
        name,
        parameters,
        type: `${componentName}/v1/${actionName}`,
    };

    const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow.definition!);

    const existingWorkflowTask = workflowDefinition.tasks?.find((task) => task.name === newTask.name);

    if (existingWorkflowTask && !actionName) {
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

        if (existingWorkflowTask.type !== newTask.type) {
            delete tasks[existingTaskIndex].parameters;
        }
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
            onSuccess: onSuccess,
        }
    );
}
