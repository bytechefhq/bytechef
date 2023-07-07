import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionModel,
} from '@/middleware/core/workflow/configuration';
import {Component1Icon} from '@radix-ui/react-icons';
import Input from 'components/Input/Input';
import React, {memo, useEffect, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Edge, MarkerType, Node, useReactFlow} from 'reactflow';

import WorkflowNodesList from '../components/WorkflowNodesList';
import useWorkflowDefinitionStore from '../stores/useWorkflowDefinitionStore';
import getFormattedName from '../utils/getFormattedName';
import getRandomId from '../utils/getRandomId';

interface ContextualMenuProps {
    components: ComponentDefinitionBasicModel[] | undefined;
    flowControls: TaskDispatcherDefinitionModel[] | undefined;
    id: string;
    edge?: boolean;
}

const ContextualMenu = ({
    components,
    edge,
    flowControls,
    id,
}: ContextualMenuProps): JSX.Element => {
    const [filter, setFilter] = useState('');

    const [filteredComponents, setFilteredComponents] = useState<
        Array<ComponentDefinitionBasicModel>
    >([]);
    const [filteredFlowControls, setFilteredFlowControls] = useState<
        Array<TaskDispatcherDefinitionModel>
    >([]);

    const {componentNames, setComponentNames} = useWorkflowDefinitionStore();

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
        if (components && flowControls) {
            setFilteredComponents(
                components.filter(
                    (component) =>
                        component.name
                            ?.toLowerCase()
                            .includes(filter.toLowerCase()) ||
                        component?.title
                            ?.toLowerCase()
                            .includes(filter.toLowerCase())
                )
            );

            setFilteredFlowControls(
                flowControls.filter(
                    (flowControl) =>
                        flowControl.name
                            ?.toLowerCase()
                            .includes(filter.toLowerCase()) ||
                        flowControl?.title
                            ?.toLowerCase()
                            .includes(filter.toLowerCase())
                )
            );
        }
    }, [components, filter, flowControls, edge]);

    return (
        <div className="nowheel">
            {typeof components === 'undefined' ||
                (typeof flowControls === 'undefined' && (
                    <div className="px-3 py-2 text-xs text-gray-500">
                        Something went wrong.
                    </div>
                ))}

            <header className="border-b border-gray-200 px-3 pt-3 text-center text-gray-600">
                <Input
                    name="contextualMenuFilter"
                    placeholder="Filter workflow nodes"
                    value={filter}
                    onChange={(event) => setFilter(event.target.value)}
                />
            </header>

            <main className="max-h-80 overflow-auto rounded-b-lg bg-gray-100">
                <WorkflowNodesList
                    components={filteredComponents}
                    flowControls={filteredFlowControls}
                    onItemClick={handleItemClick}
                />
            </main>
        </div>
    );
};

export default memo(ContextualMenu);
