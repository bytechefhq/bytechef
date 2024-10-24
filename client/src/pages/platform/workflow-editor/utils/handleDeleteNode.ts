import {SPACE} from '@/shared/constants';
import {Workflow, WorkflowTask} from '@/shared/middleware/automation/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {ComponentType, NodeType, WorkflowDefinitionType} from '@/shared/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';
import {Node, NodeProps} from 'reactflow';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';

interface HandleDeleteNodeProps {
    componentNames: Array<string>;
    currentComponent?: ComponentType;
    currentNode?: NodeType;
    data: NodeProps['data'];
    getNode: (id: string) => Node | undefined;
    id: string;
    queryClient: QueryClient;
    setCurrentComponent: (component: ComponentType | undefined) => void;
    setCurrentNode: (node: NodeType | undefined) => void;
    setWorkflow: (workflow: Workflow & WorkflowTaskDataType) => void;
    updateWorkflowMutation: UseMutationResult<Workflow, unknown, {id: string; workflow: Workflow}>;
    workflow: Workflow & WorkflowTaskDataType;
}

export default function handleDeleteNode({
    componentNames,
    currentComponent,
    currentNode,
    data,
    getNode,
    id,
    queryClient,
    setCurrentComponent,
    setCurrentNode,
    setWorkflow,
    updateWorkflowMutation,
    workflow,
}: HandleDeleteNodeProps) {
    const node = getNode(id);

    if (!node) {
        return;
    }

    if (!workflow?.definition) {
        return;
    }

    const workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition);

    const updatedTasks = workflowDefinition.tasks?.filter((task: WorkflowTask) => task.name !== data.name);

    updateWorkflowMutation.mutate(
        {
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(
                    {
                        ...workflowDefinition,
                        tasks: updatedTasks,
                    },
                    null,
                    SPACE
                ),
                version: workflow.version,
            },
        },
        {
            onSuccess: () => {
                queryClient.invalidateQueries({
                    queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
                        id: workflow.id!,
                        lastWorkflowNodeName: currentNode?.name,
                    }),
                });

                setWorkflow({
                    ...workflow,
                    componentNames: componentNames.filter((componentName) => componentName !== data.componentName),
                    tasks: updatedTasks,
                });

                if (currentNode?.name === data.name) {
                    setCurrentNode(undefined);
                }

                if (currentComponent?.workflowNodeName === data.name) {
                    setCurrentComponent(undefined);
                }
            },
        }
    );
}
