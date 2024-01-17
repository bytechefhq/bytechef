import {ComponentDefinitionApi, WorkflowModel, WorkflowTaskModel} from '@/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/queries/componentDefinitions.queries';
import {WorkflowDefinition} from '@/types/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

const SPACE = 4;

interface UpdateWorkflowRequest {
    id: string;
    workflowModel: WorkflowModel;
}

export default async function saveWorkflowDefinition(
    nodeData: {
        componentName: string;
        icon: JSX.Element;
        label?: string;
        name: string;
        parameters?: {[key: string]: object};
    },
    workflow: WorkflowModel,
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>,
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
