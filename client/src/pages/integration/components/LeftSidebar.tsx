import React, {useEffect, useState} from 'react';
import {Provider} from '@radix-ui/react-tooltip';
import {Component1Icon} from '@radix-ui/react-icons';
import {
    ComponentDefinitionBasicModel,
    DisplayModel,
    TaskDispatcherDefinitionModel,
} from '../../../middleware/definition-registry';

interface DragEvent<T = Element> extends React.MouseEvent<T, DragEventInit> {
    dataTransfer: DataTransfer;
}

interface SidebarProps {
    data: {
        components: Array<ComponentDefinitionBasicModel>;
        flowControls: Array<TaskDispatcherDefinitionModel>;
    };
    filter: string;
}

const Item = ({description, label}: DisplayModel): JSX.Element => {
    const onDragStart = (event: DragEvent, label: string) => {
        event.dataTransfer.setData('application/reactflow', label);
        event.dataTransfer.effectAllowed = 'move';
    };

    return (
        <li
            className="mb-2 flex h-[72px] cursor-pointer items-center rounded-md bg-white p-2 hover:bg-gray-50"
            draggable
            id={label}
            onDragStart={(event) => onDragStart(event, label!)}
        >
            <Component1Icon className="mr-2 h-7 w-7 flex-none" />

            <div className="flex flex-col">
                <p className="text-sm font-medium text-gray-900">{label}</p>

                {/* eslint-disable-next-line tailwindcss/no-custom-classname */}
                <p className="text-left text-xs text-gray-500 line-clamp-2">
                    {description}
                </p>
            </div>
        </li>
    );
};

const LeftSidebar: React.FC<SidebarProps> = ({data, filter}): JSX.Element => {
    const [filteredComponents, setFilteredComponents] = useState<
        Array<ComponentDefinitionBasicModel>
    >([]);
    const [filteredFlowControls, setFilteredFlowControls] = useState<
        Array<TaskDispatcherDefinitionModel>
    >([]);

    const {components, flowControls} = data;

    useEffect(() => {
        setFilteredComponents(
            components.filter(
                (component) =>
                    component.name
                        ?.toLowerCase()
                        .includes(filter.toLowerCase()) ||
                    component.display?.label
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
                    flowControl.display?.label
                        ?.toLowerCase()
                        .includes(filter.toLowerCase())
            )
        );
    }, [components, filter, flowControls]);

    return (
        <Provider>
            <div className="px-2">
                <span className="sticky top-0 z-10 block w-full bg-gray-100 p-2 text-center text-sm font-bold uppercase text-gray-500">
                    Components
                </span>

                <ul role="list" className="mb-2">
                    {filteredComponents.length ? (
                        filteredComponents.map(
                            (component: ComponentDefinitionBasicModel) => (
                                <Item
                                    description={component.display?.description}
                                    label={component.display?.label}
                                    key={component.name}
                                />
                            )
                        )
                    ) : (
                        <span className="block py-2 px-3 text-xs text-gray-500">
                            No components found.
                        </span>
                    )}
                </ul>

                <span className="sticky top-0 z-10 mt-2 block w-full bg-gray-100 p-2 text-center text-sm font-bold uppercase text-gray-500">
                    Flow Controls
                </span>

                <ul role="list" className="mb-2">
                    {filteredFlowControls.length ? (
                        filteredFlowControls.map(
                            (flowControl: TaskDispatcherDefinitionModel) => (
                                <Item
                                    description={
                                        flowControl.display?.description
                                    }
                                    label={flowControl.display?.label}
                                    key={flowControl.name}
                                />
                            )
                        )
                    ) : (
                        <span className="block py-2 px-3 text-xs text-gray-500">
                            No flow controls found.
                        </span>
                    )}
                </ul>
            </div>
        </Provider>
    );
};

export default LeftSidebar;
