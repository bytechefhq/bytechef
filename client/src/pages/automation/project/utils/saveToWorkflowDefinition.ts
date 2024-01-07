import {ComponentDefinitionApi} from '@/middleware/hermes/configuration';
import {ComponentDefinitionKeys} from '@/queries/componentDefinitions.queries';
import {WorkflowDefinitionModel, WorkflowDefinitionType} from '@/types/types';
import {QueryClient} from '@tanstack/react-query';

export default async function saveToWorkflowDefinition(
    nodeData: {
        componentName: string;
        icon: JSX.Element;
        label?: string;
        name: string;
        parameters?: Array<{
            name: string;
            value: string;
        }>;
    },
    workflowDefinition: WorkflowDefinitionModel,
    workflowId: string,
    workflowDefinitions: WorkflowDefinitionType,
    setWorkflowDefinitions: (workflowDefinition: WorkflowDefinitionType) => void,
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

    const newTask = {
        componentName,
        label,
        name,
        parameters,
        type: `${componentName}/v1/${newNodeComponentDefinition.actions?.[0].name}`,
    };

    const workflowNodeAlreadyExists = workflowDefinition.tasks?.some((task) => task.name === newTask.name);

    if (workflowNodeAlreadyExists) {
        return;
    }

    const tasks = [...(workflowDefinition.tasks || [])];

    if (index) {
        tasks.splice(index, 0, newTask);

        setWorkflowDefinitions({
            ...workflowDefinitions,
            [workflowId]: {
                ...workflowDefinition,
                tasks,
            },
        });

        return;
    }

    setWorkflowDefinitions({
        ...workflowDefinitions,
        [workflowId]: {
            ...workflowDefinition,
            tasks: [...(workflowDefinition.tasks || []), newTask],
        },
    });
}
