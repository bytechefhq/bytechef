import Input from '@/components/Input/Input';
import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionModel,
} from '@/middleware/hermes/configuration';
import WorkflowNodesTabs from '@/pages/automation/project/components/WorkflowNodesTabs';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import getFormattedName from '@/pages/automation/project/utils/getFormattedName';
import getRandomId from '@/utils/getRandomId';
import {Component1Icon} from '@radix-ui/react-icons';
import {memo, useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Edge, MarkerType, Node, useReactFlow} from 'reactflow';

type WorkflowNodesListProps = {
    edge?: boolean;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    id: string;
};

const WorkflowNodesList = memo(
    ({
        edge,
        hideActionComponents = false,
        hideTaskDispatchers = false,
        hideTriggerComponents = false,
        id,
    }: WorkflowNodesListProps) => {
        const [filter, setFilter] = useState('');

        const [
            filteredActionComponentDefinitions,
            setFilteredActionComponentDefinitions,
        ] = useState<Array<ComponentDefinitionBasicModel>>([]);

        const [
            filteredTaskDispatcherDefinitions,
            setFilteredTaskDispatcherDefinitions,
        ] = useState<Array<TaskDispatcherDefinitionModel>>([]);

        const [
            filteredTriggerComponentDefinitions,
            setFilteredTriggerComponentDefinitions,
        ] = useState<Array<ComponentDefinitionBasicModel>>([]);

        const {
            componentDefinitions,
            componentNames,
            setComponentNames,
            taskDispatcherDefinitions,
        } = useWorkflowDataStore();

        const {getEdge, getNode, getNodes, setEdges, setNodes} = useReactFlow();

        const handleItemClick = (
            clickedItem:
                | ComponentDefinitionBasicModel
                | TaskDispatcherDefinitionModel
        ) => {
            if (edge) {
                const clickedEdge = getEdge(id);

                if (!clickedEdge) {
                    return;
                }

                const nodes = getNodes();

                const newWorkflowNode = {
                    data: {
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
                        name: getFormattedName(clickedItem.name!, nodes),
                        originNodeName: clickedItem.name,
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

                setEdges((edges) =>
                    edges
                        .filter((edge) => edge.id !== id)
                        .concat([sourceEdge, targetEdge])
                );

                setNodes((nodes) => {
                    if (!nodes.find((node) => node.type === 'contextualMenu')) {
                        const previousWorkflowNode = nodes.find(
                            (node) => node.id === clickedEdge.source
                        );

                        const previousComponentNameIndex =
                            componentNames.findIndex(
                                (name) =>
                                    name === previousWorkflowNode?.data.name
                            );

                        const tempComponentNames = [...componentNames];

                        tempComponentNames.splice(
                            previousComponentNameIndex + 1,
                            0,
                            newWorkflowNode.data.name
                        );

                        setComponentNames(tempComponentNames);

                        return nodes.concat(newWorkflowNode);
                    } else {
                        return nodes;
                    }
                });
            }

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
                            const formattedNodeName = getFormattedName(
                                clickedItem.name!,
                                nodes
                            );

                            setComponentNames([
                                ...componentNames,
                                formattedNodeName,
                            ]);

                            return {
                                ...node,
                                data: {
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
                                    originNodeName: clickedItem.name,
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
        };

        useEffect(() => {
            if (componentDefinitions && taskDispatcherDefinitions) {
                setFilteredActionComponentDefinitions(
                    componentDefinitions.filter(
                        (componentDefinition) =>
                            componentDefinition?.actionsCount &&
                            (componentDefinition.name
                                ?.toLowerCase()
                                .includes(filter.toLowerCase()) ||
                                componentDefinition?.title
                                    ?.toLowerCase()
                                    .includes(filter.toLowerCase()))
                    )
                );

                setFilteredTaskDispatcherDefinitions(
                    taskDispatcherDefinitions.filter(
                        (taskDispatcherDefinition) =>
                            taskDispatcherDefinition.name
                                ?.toLowerCase()
                                .includes(filter.toLowerCase()) ||
                            taskDispatcherDefinition?.title
                                ?.toLowerCase()
                                .includes(filter.toLowerCase())
                    )
                );

                setFilteredTriggerComponentDefinitions(
                    componentDefinitions.filter(
                        (componentDefinition) =>
                            componentDefinition?.triggersCount &&
                            (componentDefinition.name
                                ?.toLowerCase()
                                .includes(filter.toLowerCase()) ||
                                componentDefinition?.title
                                    ?.toLowerCase()
                                    .includes(filter.toLowerCase()))
                    )
                );
            }
        }, [componentDefinitions, filter, taskDispatcherDefinitions, edge]);

        return (
            <div className="nowheel">
                {typeof componentDefinitions === 'undefined' ||
                    (typeof taskDispatcherDefinitions === 'undefined' && (
                        <div className="px-3 py-2 text-xs text-gray-500">
                            Something went wrong.
                        </div>
                    ))}

                <header className="border-b border-gray-200 px-3 pt-3 text-center text-gray-600">
                    <Input
                        name="contextualMenuFilter"
                        onChange={(event) => setFilter(event.target.value)}
                        placeholder="Filter workflow nodes"
                        value={filter}
                    />
                </header>

                <main className="max-h-80 overflow-auto rounded-b-lg bg-gray-100">
                    <WorkflowNodesTabs
                        actionComponentDefinitions={
                            filteredActionComponentDefinitions
                        }
                        hideActionComponents={hideActionComponents}
                        hideTaskDispatchers={hideTaskDispatchers}
                        hideTriggerComponents={hideTriggerComponents}
                        onItemClick={handleItemClick}
                        taskDispatcherDefinitions={
                            filteredTaskDispatcherDefinitions
                        }
                        triggerComponentDefinitions={
                            filteredTriggerComponentDefinitions
                        }
                    />
                </main>
            </div>
        );
    }
);

WorkflowNodesList.displayName = 'WorkflowNodesList';

export default WorkflowNodesList;
