import {ComponentDefinitionApi, WorkflowModel, WorkflowTaskModel} from '@/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/queries/platform/componentDefinitions.queries';
import {WorkflowDefinition} from '@/types/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

const SPACE = 4;

interface UpdateWorkflowRequest {
    id: string;
    workflowModel: WorkflowModel;
}

interface NodeDataType {
    actionName?: string;
    componentName: string;
    icon?: JSX.Element | string;
    label?: string;
    name: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: {[key: string]: any};
    type?: string;
}

export default async function saveWorkflowDefinition(
    nodeData: NodeDataType,
    workflow: WorkflowModel,
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>,
    index?: number
) {
    console.log('saving nodeData: ', nodeData);
    const {actionName, componentName, label, name, parameters} = nodeData;

    const queryClient = new QueryClient();

    const newNodeComponentDefinition = await queryClient.fetchQuery({
        queryFn: () => new ComponentDefinitionApi().getComponentDefinition({componentName}),
        queryKey: ComponentDefinitionKeys.componentDefinition({componentName}),
    });

    if (!newNodeComponentDefinition) {
        return;
    }

    // console.log('actionName: ', actionName);
    // console.log('newNodeComponentDefinition: ', newNodeComponentDefinition);

    const newTask: WorkflowTaskModel = {
        label,
        name,
        parameters,
        type: `${componentName}/v1/${actionName || newNodeComponentDefinition.actions?.[0].name}`,
    };

    const existingWorkflowTask = workflow.tasks?.find((task) => task.name === newTask.name);

    if (existingWorkflowTask && !actionName) {
        return;
    }

    let tasks: WorkflowTaskModel[];
    const workflowDefinition: WorkflowDefinition = JSON.parse(workflow.definition!);

    if (existingWorkflowTask) {
        const existingTaskIndex = workflowDefinition.tasks?.findIndex(
            (task) => task.name === existingWorkflowTask.name
        );

        if (existingTaskIndex === undefined) {
            return;
        }

        // console.log('existingWorkflowTask: ', existingWorkflowTask);
        // console.log('newTask: ', newTask);

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

        console.log('combinedTask: ', combinedTask);

        tasks[existingTaskIndex] = combinedTask;

        if (existingWorkflowTask.type !== newTask.type) {
            delete tasks[existingTaskIndex].parameters;
        }

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-expect-error
        delete tasks[existingTaskIndex].connections;
    } else if (index) {
        tasks = [...(workflowDefinition.tasks || [])];

        tasks.splice(index, 0, newTask);
    } else {
        tasks = [...(workflowDefinition.tasks || []), newTask];
    }

    updateWorkflowMutation.mutate({
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
    });
}
