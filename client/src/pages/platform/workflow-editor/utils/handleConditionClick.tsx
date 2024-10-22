import {TaskDispatcherDefinitionApi, Workflow} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {ClickedDefinitionType, NodeType, PropertyAllType, UpdateWorkflowMutationType} from '@/shared/types';
import {Component1Icon} from '@radix-ui/react-icons';
import {QueryClient} from '@tanstack/react-query';
import InlineSVG from 'react-inlinesvg';
import {Node} from 'reactflow';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleConditionClickProps {
    clickedItem: ClickedDefinitionType;
    currentNode?: NodeType;
    edge?: boolean;
    getNode: (id: string) => Node | undefined;
    queryClient: QueryClient;
    setNodes: (nodes: unknown) => Array<Node>;
    setWorkflow: (workflowDefinition: Workflow & WorkflowTaskDataType) => void;
    sourceNodeId: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default async function handleConditionClick({
    clickedItem,
    currentNode,
    edge,
    getNode,
    queryClient,
    setNodes,
    setWorkflow,
    sourceNodeId,
    updateWorkflowMutation,
    workflow,
}: HandleConditionClickProps) {
    const clickedTaskDispatcherDefinition = await queryClient.fetchQuery({
        queryFn: () =>
            new TaskDispatcherDefinitionApi().getTaskDispatcherDefinition({
                taskDispatcherName: clickedItem.name,
                taskDispatcherVersion: clickedItem.version,
            }),
        queryKey: ComponentDefinitionKeys.componentDefinition({
            componentName: clickedItem.name,
        }),
    });

    if (!clickedTaskDispatcherDefinition) {
        return;
    }

    const {componentNames} = workflow;

    if (edge) {
        // TODO
    } else {
        const sourceNode = getNode(sourceNodeId);

        if (!sourceNode) {
            return;
        }

        setNodes((nodes: Array<Node>) =>
            nodes.map((node) => {
                if (node.id !== sourceNodeId) {
                    return node;
                }

                const workflowNodeName = getFormattedName(clickedItem.name!, nodes);

                setWorkflow({
                    ...workflow,
                    componentNames: [...componentNames, clickedItem.name],
                    nodeNames: [...workflow.nodeNames, workflowNodeName],
                });

                const newConditionNodeData = {
                    ...clickedTaskDispatcherDefinition,
                    componentName: clickedItem.name,
                    icon: (
                        <>
                            {clickedItem.icon ? (
                                <InlineSVG className="size-9 text-gray-700" src={clickedItem.icon} />
                            ) : (
                                <Component1Icon className="size-9 text-gray-700" />
                            )}
                        </>
                    ),
                    label: clickedItem?.title,
                    name: workflowNodeName,
                    taskDispatcher: true,
                    type: `${clickedTaskDispatcherDefinition.name}/v${clickedTaskDispatcherDefinition.version}`,
                };

                saveWorkflowDefinition({
                    nodeData: {
                        ...newConditionNodeData,
                        parameters: getParametersWithDefaultValues({
                            properties: clickedTaskDispatcherDefinition?.properties as Array<PropertyAllType>,
                        }),
                    },
                    onSuccess: () => {
                        queryClient.invalidateQueries({
                            queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
                                id: workflow.id!,
                                lastWorkflowNodeName: currentNode?.name,
                            }),
                        });
                    },
                    queryClient,
                    updateWorkflowMutation,
                    workflow,
                });

                return {
                    ...node,
                    data: newConditionNodeData,
                    type: 'workflow',
                };
            })
        );
    }
}
