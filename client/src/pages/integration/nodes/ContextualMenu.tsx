import {Component1Icon} from '@radix-ui/react-icons';
import Input from 'components/Input/Input';
import {ComponentDefinitionModel} from 'data-access/component-definition';
import {TaskDispatcherDefinitionModel} from 'data-access/task-dispatcher-definition';
import React, {HTMLAttributes, memo, useEffect, useState} from 'react';
import {Edge, MarkerType, Node, useReactFlow} from 'reactflow';

const uuid = (): string =>
    new Date().getTime().toString(36) + Math.random().toString(36).slice(2);

interface ContextualMenuProps {
    components: ComponentDefinitionModel[] | undefined;
    flowControls: TaskDispatcherDefinitionModel[] | undefined;
    id: string;
}

interface ContextualMenuItemProps extends HTMLAttributes<HTMLLIElement> {
    item: ComponentDefinitionModel | TaskDispatcherDefinitionModel;
    onClick: () => void;
}

const ContextualMenuItem = ({item, onClick}: ContextualMenuItemProps) => (
    <li
        className="flex cursor-pointer items-center py-2 px-3 hover:bg-gray-50"
        onClick={onClick}
    >
        <Component1Icon className="mr-3 h-6 w-6 flex-none" />

        <div>
            <span className="flex items-center  text-sm">
                {item.display?.label}
            </span>

            {item.display?.description && (
                // eslint-disable-next-line tailwindcss/no-custom-classname
                <p className="text-left text-xs text-gray-500 line-clamp-2">
                    {item.display.description}
                </p>
            )}
        </div>
    </li>
);

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
        const childPlaceholderId = uuid();

        // create a placeholder node that will be added as a child of the clicked node
        const childPlaceholderNode = {
            id: childPlaceholderId,
            position: {
                x: placeholderNode.position.x,
                y: placeholderNode.position.y,
            },
            type: 'placeholder',
            data: {label: '+'},
            style: {
                zIndex: 9999,
            },
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
                            markerEnd: {
                                type: MarkerType.ArrowClosed,
                            },
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
        <div className="nowheel rounded-md bg-white shadow-md">
            {typeof components === 'undefined' ||
                (typeof flowControls === 'undefined' && (
                    <div className="py-2 px-3 text-xs text-gray-500">
                        Something went wrong.
                    </div>
                ))}

            <header className="border-b border-gray-200 px-3 pt-2 text-center font-bold text-gray-600">
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
                                <ContextualMenuItem
                                    item={filteredItem}
                                    key={filteredItem.name}
                                    onClick={() =>
                                        handleItemClick(filteredItem)
                                    }
                                />
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
