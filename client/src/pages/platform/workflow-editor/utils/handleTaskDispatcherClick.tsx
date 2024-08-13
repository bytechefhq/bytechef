import {TaskDispatcherDefinitionApi, WorkflowModel} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {ClickedItemType, NodeType, PropertyType, UpdateWorkflowMutationType} from '@/shared/types';
import {getRandomId} from '@/shared/util/random-utils';
import {Component1Icon} from '@radix-ui/react-icons';
import {QueryClient} from '@tanstack/react-query';
import InlineSVG from 'react-inlinesvg';
import {Edge, Node} from 'reactflow';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleTaskDispatcherClickProps {
    clickedItem: ClickedItemType;
    currentNode?: NodeType;
    edge?: boolean;
    getNode: (id: string) => Node | undefined;
    id: string;
    queryClient: QueryClient;
    setEdges: (edges: unknown) => Array<Edge>;
    setNodes: (nodes: unknown) => Array<Node>;
    setWorkflow: (workflowDefinition: WorkflowModel & WorkflowTaskDataType) => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: WorkflowModel & WorkflowTaskDataType;
}

export default async function handleTaskDispatcherClick({
    clickedItem,
    currentNode,
    edge,
    getNode,
    id,
    queryClient,
    setEdges,
    setNodes,
    setWorkflow,
    updateWorkflowMutation,
    workflow,
}: HandleTaskDispatcherClickProps) {
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
        const sourceNode = getNode(id);

        if (!sourceNode) {
            return;
        }

        const sourceNodeId = sourceNode.id;

        const bottomPlaceholderNodeId = getRandomId();

        const bottomPlaceholderNode = {
            data: {label: '+'},
            id: bottomPlaceholderNodeId,
            position: {
                x: sourceNode.position.x,
                y: sourceNode.position.y,
            },
            style: {
                zIndex: 9999,
            },
            type: 'placeholder',
        };

        setNodes((nodes: Array<Node>) =>
            nodes
                .map((node) => {
                    if (node.id === sourceNodeId) {
                        const workflowNodeName = getFormattedName(clickedItem.name!, nodes);

                        setWorkflow({
                            ...workflow,
                            componentNames: [...componentNames, clickedItem.name],
                            nodeNames: [...workflow.nodeNames, workflowNodeName],
                        });

                        const newWorkflowNodeData = {
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

                        saveWorkflowDefinition(
                            {
                                ...newWorkflowNodeData,
                                parameters: getParametersWithDefaultValues({
                                    properties: clickedTaskDispatcherDefinition?.properties as Array<PropertyType>,
                                }),
                                type: `${clickedTaskDispatcherDefinition.name}/v${clickedTaskDispatcherDefinition.version}`,
                            },
                            workflow!,
                            updateWorkflowMutation,
                            undefined,
                            () => {
                                queryClient.invalidateQueries({
                                    queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
                                        id: workflow.id!,
                                        lastWorkflowNodeName: currentNode?.name,
                                    }),
                                });
                            }
                        );

                        return {
                            ...node,
                            data: newWorkflowNodeData,
                            type: clickedItem.name,
                        };
                    }

                    return node;
                })
                .concat([bottomPlaceholderNode])
        );

        const leftConditionEdge = {
            id: `${sourceNodeId}left=>${bottomPlaceholderNodeId}`,
            source: sourceNodeId,
            sourceHandle: 'left',
            target: bottomPlaceholderNodeId,
            type: 'condition',
        };

        const rightConditionEdge = {
            id: `${sourceNodeId}right=>${bottomPlaceholderNodeId}`,
            source: sourceNodeId,
            sourceHandle: 'right',
            target: bottomPlaceholderNodeId,
            type: 'condition',
        };

        setEdges((edges: Array<Edge>) => edges.concat([leftConditionEdge, rightConditionEdge]));
    }
}
