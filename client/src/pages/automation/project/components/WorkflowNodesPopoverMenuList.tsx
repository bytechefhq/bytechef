import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from '@/middleware/platform/configuration';
import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import WorkflowNodesTabs from '@/pages/automation/project/components/WorkflowNodesTabs';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import getFormattedName from '@/pages/automation/project/utils/getFormattedName';
import {WorkflowKeys} from '@/queries/automation/workflows.queries';
import {ClickedItemType} from '@/types/types';
import getRandomId from '@/utils/getRandomId';
import {Component1Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {memo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Edge, MarkerType, Node, useReactFlow} from 'reactflow';

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
        const {projectId} = useWorkflowDataStore();

        const queryClient = useQueryClient();

        const updateWorkflowMutation = useUpdateWorkflowMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({queryKey: WorkflowKeys.projectWorkflows(projectId!)});

                queryClient.invalidateQueries({
                    queryKey: WorkflowKeys.workflow(workflow.id!),
                });
            },
        });

        const {setWorkflow, workflow} = useWorkflowDataStore();

        const {componentNames} = workflow;

        const {getEdge, getNode, getNodes, setEdges, setNodes} = useReactFlow();

        const handleItemClick = (clickedItem: ClickedItemType) => {
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
                    });

                    const previousWorkflowNodeIndex = nodes.findIndex((node) => node.id === clickedEdge.source);

                    const tempNodes = [...nodes];

                    tempNodes.splice(previousWorkflowNodeIndex + 1, 0, newWorkflowNode);

                    saveWorkflowDefinition(
                        newWorkflowNode.data,
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
                                const formattedNodeName = getFormattedName(clickedItem.name!, nodes);

                                setWorkflow({
                                    ...workflow,
                                    componentNames: [...componentNames, clickedItem.name],
                                });

                                return {
                                    ...node,
                                    data: {
                                        componentName: clickedItem.name,
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
                                        label: clickedItem?.title,
                                        name: formattedNodeName,
                                        type: node.data?.type,
                                    },
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
