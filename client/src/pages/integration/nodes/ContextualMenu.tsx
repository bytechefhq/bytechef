import {Component1Icon, Cross1Icon} from '@radix-ui/react-icons';
import Input from 'components/Input/Input';
import {ComponentDefinitionModel} from 'data-access/component-definition';
import {TaskDispatcherDefinitionModel} from 'data-access/task-dispatcher-definition';
import React, {memo, useEffect, useState} from 'react';
import {Edge, Node, NodeProps, useReactFlow} from 'reactflow';

const uuid = (): string =>
    new Date().getTime().toString(36) + Math.random().toString(36).slice(2);

const ContextualMenu = ({data, id}: NodeProps): JSX.Element => {
    const [filter, setFilter] = useState('');
    const [filteredNodes, setFilteredNodes] = useState<
        Array<ComponentDefinitionModel | TaskDispatcherDefinitionModel>
    >([]);

    const {getNode, setEdges} = useReactFlow();

    const {components, flowControls, label, setNodes} = data;

    const handleCloseClick = () => {
        setNodes((nodes: Node[]) =>
            nodes.filter((node) => node.data.label !== label)
        );
    };

    const handleItemClick = (
        filteredItem: ComponentDefinitionModel | TaskDispatcherDefinitionModel
    ) => {
        const contextualMenuNode = getNode(id);

        if (!contextualMenuNode) {
            return;
        }

        const {placeholderId} = contextualMenuNode.data;

        const placeholderNode = getNode(placeholderId);

        if (!placeholderNode) {
            return;
        }

        // create a unique id for the placeholder node that will be added as a child of the clicked node
        const childPlaceholderId = uuid();

        // create a placeholder node that will be added as a child of the clicked node
        const childPlaceholderNode = {
            id: childPlaceholderId,
            // the placeholder is placed at the position of the clicked node
            // the layout function will animate it to its new position
            position: {
                x: placeholderNode.position.x,
                y: placeholderNode.position.y,
            },
            type: 'placeholder',
            data: {label: '+'},
        };

        // we need a connection from the clicked node to the new placeholder
        const childPlaceholderEdge = {
            id: `${placeholderId}=>${childPlaceholderId}`,
            source: placeholderId,
            target: childPlaceholderId,
            type: 'placeholder',
        };

        setNodes((nodes: Node[]) =>
            nodes
                .map((node) => {
                    // here we are changing the type of the clicked node from placeholder to workflow
                    if (node.id === placeholderId) {
                        return {
                            ...node,
                            type: 'workflow',
                            data: {label: filteredItem.display?.label},
                        };
                    }
                    return node;
                })
                // add the new placeholder node
                .concat([childPlaceholderNode])
        );

        setEdges((edges: Edge[]) =>
            edges
                .map((edge) => {
                    // here we are changing the type of the connecting edge from placeholder to workflow
                    if (edge.target === id) {
                        return {
                            ...edge,
                            type: 'workflow',
                        };
                    }
                    return edge;
                })
                // add the new placeholder edge
                .concat([childPlaceholderEdge])
        );
    };

    useEffect(() => {
        setFilteredNodes(
            [...components, ...flowControls].filter(
                (item) =>
                    item.name.toLowerCase().includes(filter.toLowerCase()) ||
                    item.display.label
                        .toLowerCase()
                        .includes(filter.toLowerCase())
            )
        );
    }, [components, flowControls, filter]);

    return (
        // eslint-disable-next-line tailwindcss/no-custom-classname
        <div className="nowheel rounded-md bg-white shadow-md" draggable>
            <header className="flex items-center px-3 pt-2 text-center font-bold text-gray-600">
                <Input
                    name="contextualMenuFilter"
                    placeholder="Filter nodes"
                    value={filter}
                    onChange={(event) => setFilter(event.target.value)}
                />

                <Cross1Icon className="ml-2 mb-3" onClick={handleCloseClick} />
            </header>

            <main className="max-h-64 overflow-auto">
                <ul>
                    {filteredNodes.length ? (
                        filteredNodes.map(
                            (
                                filteredItem:
                                    | ComponentDefinitionModel
                                    | TaskDispatcherDefinitionModel
                            ) => (
                                <li
                                    className="border-t border-t-slate-300 py-2 px-4"
                                    key={filteredItem.name}
                                    onClick={() =>
                                        handleItemClick(filteredItem)
                                    }
                                >
                                    <span className="flex items-center  text-sm">
                                        <Component1Icon className="mr-1" />

                                        {filteredItem.display?.label}
                                    </span>
                                </li>
                            )
                        )
                    ) : (
                        <div className="py-2 px-3 text-xs text-gray-500">
                            No items found.
                        </div>
                    )}
                </ul>
            </main>
        </div>
    );
};

export default memo(ContextualMenu);
