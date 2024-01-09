import {
    ComponentDefinitionApi,
    UpdateWorkflowRequest,
    WorkflowModel,
    WorkflowTaskModel,
} from '@/middleware/hermes/configuration';
import {ComponentDefinitionKeys} from '@/queries/componentDefinitions.queries';
import {WorkflowDefinition} from '@/types/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

const SPACE = 4;

export default async function saveToWorkflowDefinition(
    nodeData: {
        componentName: string;
        icon: JSX.Element;
        label?: string;
        name: string;
        parameters?: {[key: string]: object};
    },
    workflow: WorkflowModel,
    updateWorkflowMutationMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>,
    index?: number
) {
    const {componentName, label, name, parameters} = nodeData;

    const queryClient = new QueryClient();

    const newNodeComponentDefinition = await queryClient.fetchQuery({
        queryFn: () => new ComponentDefinitionApi().getComponentDefinition({componentName}),
        queryKey: ComponentDefinitionKeys.componentDefinition({componentName}),
    });

    if (!newNodeComponentDefinition) {
        return;
    }

    const newTask: WorkflowTaskModel = {
        label,
        name,
        parameters,
        type: `${componentName}/v1/${newNodeComponentDefinition.actions?.[0].name}`,
    };

    const workflowNodeAlreadyExists = workflow.tasks?.some((task) => task.name === newTask.name);

    if (workflowNodeAlreadyExists) {
        return;
    }

    let tasks: WorkflowTaskModel[];
    const workflowDefinition: WorkflowDefinition = JSON.parse(workflow.definition!);

    if (index) {
        tasks = [...(workflowDefinition.tasks || [])];

        tasks.splice(index, 0, newTask);
    } else {
        tasks = [...(workflowDefinition.tasks || []), newTask];
    }

    updateWorkflowMutationMutation.mutate({
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
