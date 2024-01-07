import Input from '@/components/Input/Input';
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from '@/middleware/hermes/configuration';
import WorkflowNodesTabs from '@/pages/automation/project/components/WorkflowNodesTabs';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import getFormattedName from '@/pages/automation/project/utils/getFormattedName';
import {ClickedItemType} from '@/types/types';
import getRandomId from '@/utils/getRandomId';
import {Component1Icon} from '@radix-ui/react-icons';
import {memo, useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Edge, MarkerType, Node, useReactFlow} from 'reactflow';

import useWorkflowDefinitionStore from '../stores/useWorkflowDefinitionStore';
import saveToWorkflowDefinition from '../utils/saveToWorkflowDefinition';

type WorkflowNodesListProps = {
    edge?: boolean;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    id: string;
};

const WorkflowNodesPopoverMenuList = memo(
    ({
        edge,
        hideActionComponents = false,
        hideTaskDispatchers = false,
        hideTriggerComponents = false,
        id,
    }: WorkflowNodesListProps) => {
        const [filter, setFilter] = useState('');

        useEffect(() => {
            if (filter) {
                setFilter(filter.toLowerCase());
            }
        }, [filter]);

        const [filteredActionComponentDefinitions, setFilteredActionComponentDefinitions] = useState<
            Array<ComponentDefinitionBasicModel>
        >([]);

        const [filteredTaskDispatcherDefinitions, setFilteredTaskDispatcherDefinitions] = useState<
            Array<TaskDispatcherDefinitionModel>
        >([]);

        const [filteredTriggerComponentDefinitions, setFilteredTriggerComponentDefinitions] = useState<
            Array<ComponentDefinitionBasicModel>
        >([]);

        const {componentDefinitions, componentNames, currentWorkflowId, setComponentNames, taskDispatcherDefinitions} =
            useWorkflowDataStore();

        const {setWorkflowDefinitions, workflowDefinitions} = useWorkflowDefinitionStore();

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
                                    <InlineSVG className="h-9 w-9 text-gray-700" src={clickedItem.icon} />
                                ) : (
                                    <Component1Icon className="h-9 w-9 text-gray-700" />
                                )}
                            </>
                        ),
                        label: clickedItem?.title,
                        name: getFormattedName(clickedItem.name!, nodes),
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

                    const currentWorkflowDefinition = workflowDefinitions[currentWorkflowId!];

                    setComponentNames(tempComponentNames);

                    const previousWorkflowNodeIndex = nodes.findIndex((node) => node.id === clickedEdge.source);

                    const tempNodes = [...nodes];

                    tempNodes.splice(previousWorkflowNodeIndex + 1, 0, newWorkflowNode);

                    saveToWorkflowDefinition(
                        newWorkflowNode.data,
                        currentWorkflowDefinition,
                        currentWorkflowId,
                        workflowDefinitions,
                        setWorkflowDefinitions,
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

                                setComponentNames([...componentNames, clickedItem.name]);

                                return {
                                    ...node,
                                    data: {
                                        componentName: clickedItem.name,
                                        icon: (
                                            <>
                                                {clickedItem.icon ? (
                                                    <InlineSVG
                                                        className="h-9 w-9 text-gray-700"
                                                        src={clickedItem.icon}
                                                    />
                                                ) : (
                                                    <Component1Icon className="h-9 w-9 text-gray-700" />
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

        useEffect(() => {
            if (taskDispatcherDefinitions) {
                setFilteredTaskDispatcherDefinitions(
                    taskDispatcherDefinitions.filter(
                        ({name, title}) => name?.toLowerCase().includes(filter) || title?.toLowerCase().includes(filter)
                    )
                );
            }
        }, [taskDispatcherDefinitions, filter, edge]);

        useEffect(() => {
            if (componentDefinitions) {
                setFilteredActionComponentDefinitions(
                    componentDefinitions.filter(
                        ({actionsCount, name, title}) =>
                            actionsCount &&
                            (name?.toLowerCase().includes(filter) || title?.toLowerCase().includes(filter))
                    )
                );

                setFilteredTriggerComponentDefinitions(
                    componentDefinitions.filter(
                        ({name, title, triggersCount}) =>
                            triggersCount &&
                            (name?.toLowerCase().includes(filter) || title?.toLowerCase().includes(filter))
                    )
                );
            }
        }, [componentDefinitions, filter, edge]);

        return (
            <div className="nowheel">
                {typeof componentDefinitions === 'undefined' ||
                    (typeof taskDispatcherDefinitions === 'undefined' && (
                        <div className="px-3 py-2 text-xs text-gray-500">Something went wrong.</div>
                    ))}

                <header className="border-b border-gray-200 px-3 pt-3 text-center text-gray-600">
                    <Input
                        name="workflowNodeFilter"
                        onChange={(event) => setFilter(event.target.value)}
                        placeholder="Filter workflow nodes"
                        value={filter}
                    />
                </header>

                <main className="max-h-80 overflow-auto rounded-b-lg bg-gray-100">
                    <WorkflowNodesTabs
                        actionComponentDefinitions={filteredActionComponentDefinitions}
                        hideActionComponents={hideActionComponents}
                        hideTaskDispatchers={hideTaskDispatchers}
                        hideTriggerComponents={hideTriggerComponents}
                        onItemClick={handleItemClick}
                        taskDispatcherDefinitions={filteredTaskDispatcherDefinitions}
                        triggerComponentDefinitions={filteredTriggerComponentDefinitions}
                    />
                </main>
            </div>
        );
    }
);

WorkflowNodesPopoverMenuList.displayName = 'WorkflowNodesPopoverMenuList';

export default WorkflowNodesPopoverMenuList;
