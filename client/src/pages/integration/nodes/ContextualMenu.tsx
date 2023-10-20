import {Component1Icon} from '@radix-ui/react-icons';
import Input from 'components/Input/Input';
import {ComponentDefinitionModel} from 'data-access/component-definition';
import {TaskDispatcherDefinitionModel} from 'data-access/task-dispatcher-definition';
import React, {memo, useEffect, useState} from 'react';
import {Edge, Node, useReactFlow} from 'reactflow';

const uuid = (): string =>
    new Date().getTime().toString(36) + Math.random().toString(36).slice(2);

interface ContextualMenuProps {
    components: ComponentDefinitionModel[] | undefined;
    flowControls: TaskDispatcherDefinitionModel[] | undefined;
    id: string;
}

const ContextualMenu = ({
    components,
    flowControls,
    id,
}: ContextualMenuProps): JSX.Element => {
    const [filter, setFilter] = useState('');
    const [filteredNodes, setFilteredNodes] = useState<
        Array<ComponentDefinitionModel | TaskDispatcherDefinitionModel>
    >([]);

    const {getNode, setEdges, setNodes} = useReactFlow();

    const handleItemClick = (
        clickedItem: ComponentDefinitionModel | TaskDispatcherDefinitionModel
    ) => {
        const placeholderNode = getNode(id);

        if (!placeholderNode) {
            return;
        }

        const placeholderId = placeholderNode.id;

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
                            data: {
                                label: clickedItem.display?.label,
                                name: clickedItem?.name,
                                icon: (
                                    <Component1Icon className="h-8 w-8 text-gray-700" />
                                ),
                            },
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
        if (components && flowControls) {
            setFilteredNodes(
                [...components, ...flowControls].filter(
                    (item) =>
                        item.name
                            ?.toLowerCase()
                            .includes(filter.toLowerCase()) ||
                        item.display?.label
                            ?.toLowerCase()
                            .includes(filter.toLowerCase())
                )
            );
        }
    }, [components, flowControls, filter]);

    return (
        // eslint-disable-next-line tailwindcss/no-custom-classname
        <div className="nowheel rounded-md bg-white shadow-md" draggable>
            {typeof components === 'undefined' ||
                (typeof flowControls === 'undefined' && (
                    <div className="py-2 px-3 text-xs text-gray-500">
                        Something went wrong.
                    </div>
                ))}

            <header className="flex items-center px-3 pt-2 text-center font-bold text-gray-600">
                <Input
                    name="contextualMenuFilter"
                    placeholder="Filter nodes"
                    value={filter}
                    onChange={(event) => setFilter(event.target.value)}
                />
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
                                    className="py-2 px-4"
                                    key={filteredItem.name}
                                    onClick={() =>
                                        handleItemClick(filteredItem)
                                    }
                                >
                                    <span className="flex items-center  text-sm">
                                        <Component1Icon className="mr-1" />

                                        {filteredItem.display?.label}
                                    </span>

                                    {/* eslint-disable-next-line tailwindcss/no-custom-classname */}
                                    <p className="text-left text-xs text-gray-500 line-clamp-2">
                                        {filteredItem.display?.description}
                                    </p>
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
