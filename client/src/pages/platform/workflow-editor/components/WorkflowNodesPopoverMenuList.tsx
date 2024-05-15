import {
    ActionDefinitionApi,
    ComponentDefinitionApi,
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionModel,
} from '@/middleware/platform/configuration';
import WorkflowNodesTabs from '@/pages/platform/workflow-editor/components/WorkflowNodesTabs';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import getFormattedName from '@/pages/platform/workflow-editor/utils/getFormattedName';
import {ActionDefinitionKeys} from '@/queries/platform/actionDefinitions.queries';
import {ComponentDefinitionKeys} from '@/queries/platform/componentDefinitions.queries';
import {ClickedItemType, PropertyType} from '@/types/types';
import getRandomId from '@/utils/getRandomId';
import {Component1Icon} from '@radix-ui/react-icons';
import {QueryClient} from '@tanstack/react-query';
import {memo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Edge, MarkerType, Node, useReactFlow} from 'reactflow';

import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';

interface WorkflowNodesListProps {
    actionComponentDefinitions: Array<ComponentDefinitionBasicModel>;
    edge?: boolean;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    id: string;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionModel>;
    triggerComponentDefinitions: Array<ComponentDefinitionBasicModel>;
}

const WorkflowNodesPopoverMenuList = memo(
    ({
        actionComponentDefinitions,
        edge,
        hideActionComponents = false,
        hideTaskDispatchers = false,
        hideTriggerComponents = false,
        id,
        taskDispatcherDefinitions,
        triggerComponentDefinitions,
    }: WorkflowNodesListProps) => {
        const {setLatestComponentDefinition, setWorkflow, workflow} = useWorkflowDataStore();
        const {setCurrentNode} = useWorkflowNodeDetailsPanelStore();

        const {getEdge, getNode, getNodes, setEdges, setNodes} = useReactFlow();

        const {updateWorkflowMutation} = useWorkflowMutation();

        const queryClient = new QueryClient();

        const {componentNames} = workflow;

        const handleItemClick = async (clickedItem: ClickedItemType) => {
            const clickedComponentDefinition = await queryClient.fetchQuery({
                queryFn: () =>
                    new ComponentDefinitionApi().getComponentDefinition({
                        componentName: clickedItem.name,
                    }),
                queryKey: ComponentDefinitionKeys.componentDefinition({
                    componentName: clickedItem.name,
                }),
            });

            if (!clickedComponentDefinition) {
                return;
            }

            setLatestComponentDefinition(clickedComponentDefinition);

            const getActionDefinitionRequest = {
                actionName: clickedComponentDefinition.actions?.[0].name as string,
                componentName: clickedItem.name,
                componentVersion: clickedComponentDefinition.version,
            };

            const clickedComponentActionDefinition = await queryClient.fetchQuery({
                queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(getActionDefinitionRequest),
                queryKey: ActionDefinitionKeys.actionDefinition(getActionDefinitionRequest),
            });

            if (edge) {
                const clickedEdge = getEdge(id);

                if (!clickedEdge) {
                    return;
                }

                const nodes = getNodes();

                const newWorkflowNode = {
                    data: {
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
                        name: getFormattedName(clickedItem.name!, nodes),
                        type: 'workflow',
                    },
                    id: getRandomId(),
                    position: {
                        x: 0,
                        y: 0,
                    },
                    type: 'workflow',
                };

                const sourceEdge = {
                    id: `${clickedEdge.source}->${newWorkflowNode.id}`,
                    source: clickedEdge.source,
                    target: newWorkflowNode.id,
                    type: 'workflow',
                };

                const targetEdge = {
                    id: `${newWorkflowNode.id}->${clickedEdge.target}`,
                    source: newWorkflowNode.id,
                    target: clickedEdge.target,
                    type: 'workflow',
                };

                setNodes((nodes) => {
                    const previousWorkflowNode = nodes.find((node) => node.id === clickedEdge.source);

                    const previousComponentNameIndex = componentNames.findIndex(
                        (name) => name === previousWorkflowNode?.data.componentName
                    );

                    const tempComponentNames = [...componentNames];

                    tempComponentNames.splice(previousComponentNameIndex + 1, 0, newWorkflowNode.data.componentName);

                    setWorkflow({
                        ...workflow,
                        componentNames: tempComponentNames,
                        nodeNames: [...workflow.nodeNames, newWorkflowNode.data.name],
                    });

                    const previousWorkflowNodeIndex = nodes.findIndex((node) => node.id === clickedEdge.source);

                    const tempNodes = [...nodes];

                    tempNodes.splice(previousWorkflowNodeIndex + 1, 0, newWorkflowNode);

                    saveWorkflowDefinition(
                        {
                            ...newWorkflowNode.data,
                            parameters: getParametersWithDefaultValues({
                                properties: clickedComponentActionDefinition?.properties as Array<PropertyType>,
                            }),
                            type: `${clickedComponentDefinition.name}/${clickedComponentDefinition.version}/${clickedComponentDefinition.actions?.[0].name}`,
                        },
                        workflow!,
                        updateWorkflowMutation,
                        previousWorkflowNodeIndex
                    );

                    return tempNodes;
                });

                setEdges((edges) => edges.filter((edge) => edge.id !== id).concat([sourceEdge, targetEdge]));
            } else {
                const placeholderNode = getNode(id);

                if (!placeholderNode) {
                    return;
                }

                if (clickedItem.trigger) {
                    setNodes((nodes: Node[]) =>
                        nodes.map((node) => {
                            if (node.id === placeholderNode.id) {
                                setWorkflow({
                                    ...workflow,
                                    componentNames: [clickedItem.name, ...componentNames.slice(1)],
                                });

                                const newTriggerNode = {
                                    ...node,
                                    data: {
                                        ...node.data,
                                        componentName: clickedItem.name,
                                        connections: undefined,
                                        description: clickedItem.description,
                                        icon: (
                                            <>
                                                {clickedItem.icon ? (
                                                    <InlineSVG
                                                        className="size-9 text-gray-700"
                                                        src={clickedItem.icon}
                                                    />
                                                ) : (
                                                    <Component1Icon className="size-9 text-gray-700" />
                                                )}
                                            </>
                                        ),
                                        id: getFormattedName(clickedItem.name!, nodes),
                                        label: clickedItem?.title,
                                        name: getFormattedName(clickedItem.name!, nodes),
                                        operationName: clickedComponentDefinition.triggers?.[0].name,
                                        parameters: undefined,
                                        trigger: true,
                                        type: `${clickedItem.name}/v1/${clickedComponentDefinition.triggers?.[0].name}`,
                                    },
                                    id: getFormattedName(clickedItem.name!, nodes),
                                    type: 'workflow',
                                };

                                setCurrentNode(newTriggerNode.data);

                                saveWorkflowDefinition(newTriggerNode.data, workflow, updateWorkflowMutation);

                                return newTriggerNode;
                            }

                            return node;
                        })
                    );

                    return;
                }

                const placeholderId = placeholderNode.id;
                const childPlaceholderId = getRandomId();

                const childPlaceholderNode = {
                    data: {label: '+'},
                    id: childPlaceholderId,
                    position: {
                        x: placeholderNode.position.x,
                        y: placeholderNode.position.y,
                    },
                    style: {
                        zIndex: 9999,
                    },
                    type: 'placeholder',
                };

                const childPlaceholderEdge = {
                    id: `${placeholderId}=>${childPlaceholderId}`,
                    source: placeholderId,
                    target: childPlaceholderId,
                    type: 'placeholder',
                };

                setNodes((nodes: Node[]) =>
                    nodes
                        .map((node) => {
                            if (node.id === placeholderId) {
                                const workflowNodeName = getFormattedName(clickedItem.name!, nodes);

                                setWorkflow({
                                    ...workflow,
                                    componentNames: [...componentNames, clickedItem.name],
                                    nodeNames: [...workflow.nodeNames, workflowNodeName],
                                });

                                const newWorkflowNodeData = {
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
                                    type: `${clickedComponentDefinition.name}/v${clickedComponentDefinition.version}/${clickedComponentDefinition.actions?.[0].name}`,
                                };

                                saveWorkflowDefinition(
                                    {
                                        ...newWorkflowNodeData,
                                        parameters: getParametersWithDefaultValues({
                                            properties:
                                                clickedComponentActionDefinition?.properties as Array<PropertyType>,
                                        }),
                                        type: `${clickedComponentDefinition.name}/${clickedComponentDefinition.version}/${clickedComponentDefinition.actions?.[0].name}`,
                                    },
                                    workflow!,
                                    updateWorkflowMutation
                                );

                                return {
                                    ...node,
                                    data: newWorkflowNodeData,
                                    type: 'workflow',
                                };
                            }

                            return node;
                        })
                        .concat([childPlaceholderNode])
                );

                setEdges((edges: Edge[]) =>
                    edges
                        .map((edge) => {
                            if (edge.target === id) {
                                return {
                                    ...edge,
                                    markerEnd: {
                                        type: MarkerType.ArrowClosed,
                                    },
                                    type: 'workflow',
                                };
                            }

                            return edge;
                        })
                        .concat([childPlaceholderEdge])
                );
            }
        };

        return (
            <main className="h-96 rounded-b-lg bg-gray-100">
                <WorkflowNodesTabs
                    actionComponentDefinitions={actionComponentDefinitions}
                    hideActionComponents={hideActionComponents}
                    hideTaskDispatchers={hideTaskDispatchers}
                    hideTriggerComponents={hideTriggerComponents}
                    onItemClick={handleItemClick}
                    popover
                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                    triggerComponentDefinitions={triggerComponentDefinitions}
                />
            </main>
        );
    }
);

WorkflowNodesPopoverMenuList.displayName = 'WorkflowNodesPopoverMenuList';

export default WorkflowNodesPopoverMenuList;
