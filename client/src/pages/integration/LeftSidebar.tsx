import React, {useEffect, useState} from 'react';
import {Provider} from '@radix-ui/react-tooltip';
import {Component1Icon} from '@radix-ui/react-icons';
import {
    ComponentDefinitionModel,
    DisplayModel,
} from 'data-access/component-definition';
import {TaskDispatcherDefinitionModel} from 'data-access/task-dispatcher-definition';

interface DragEvent<T = Element> extends React.MouseEvent<T, DragEventInit> {
    dataTransfer: DataTransfer;
}

interface SidebarProps {
    data: {
        components: Array<ComponentDefinitionModel>;
        flowControls: Array<TaskDispatcherDefinitionModel>;
    };
    filter: string;
    view: string;
}

const Item = ({description, label}: DisplayModel): JSX.Element => {
    const onDragStart = (event: DragEvent, label: string) => {
        event.dataTransfer.setData('application/reactflow', label);
        event.dataTransfer.effectAllowed = 'move';
    };

    return (
        <li
            className="my-2 flex h-[72px] items-center rounded-md bg-white p-2 hover:cursor-pointer hover:bg-gray-50"
            draggable
            id={label}
            onDragStart={(event) => onDragStart(event, label!)}
        >
            <Component1Icon className="mr-2 h-7 w-7 flex-none " />

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

const LeftSidebar: React.FC<SidebarProps> = ({
    data,
    filter,
    view = 'components',
}): JSX.Element => {
    const [filteredComponents, setFilteredComponents] = useState<
        Array<ComponentDefinitionModel>
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
                <ul role="list" className="mb-2">
                    {view === 'components'
                        ? filteredComponents.map(
                              (component: ComponentDefinitionModel) => (
                                  <Item
                                      description={
                                          component.display?.description
                                      }
                                      label={component.display?.label}
                                      key={component.name}
                                  />
                              )
                          )
                        : filteredFlowControls.map(
                              (flowControl: TaskDispatcherDefinitionModel) => (
                                  <Item
                                      description={
                                          flowControl.display?.description
                                      }
                                      label={flowControl.display?.label}
                                      key={flowControl.name}
                                  />
                              )
                          )}
                </ul>
            </div>
        </Provider>
    );
};

export default LeftSidebar;
