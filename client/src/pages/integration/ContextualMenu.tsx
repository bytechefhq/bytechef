import {Component1Icon, Cross1Icon} from '@radix-ui/react-icons';
import Input from 'components/Input/Input';
import {ComponentDefinitionModel} from 'data-access/component-definition';
import {TaskDispatcherDefinitionModel} from 'data-access/task-dispatcher-definition';
import React, {memo, useEffect, useState} from 'react';
import {Node, NodeProps} from 'reactflow';

const ContextualMenu = ({data}: NodeProps): JSX.Element => {
    const [filter, setFilter] = useState('');
    const [filteredNodes, setFilteredNodes] = useState<
        Array<ComponentDefinitionModel | TaskDispatcherDefinitionModel>
    >([]);

    const {components, flowControls, label, setNodes} = data;

    const handleCloseClick = () => {
        setNodes((nodes: Node[]) =>
            nodes.filter((node) => node.data.label !== label)
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
        <div className="nowheel rounded-md bg-white shadow-md">
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
