import React from 'react';
import {Provider} from '@radix-ui/react-tooltip';
import {Component1Icon} from '@radix-ui/react-icons';

export interface ComponentType {
    name: string;
    display: {
        icon: string;
        description: string;
        tags: string[];
        category: string;
        label: string;
        subtitle: string;
    };
    version: number;
}

export interface FlowControlType {
    name: string;
    resources: object;
    properties: object[];
    version: number;
    output: object[];
    display: {
        description: string;
        label: string;
    };
    taskProperties: object[];
}

interface SidebarItemProps {
    data: {
        display: {
            description: string;
            label: string;
        };
    };
}

interface DragEvent<T = Element> extends React.MouseEvent<T, DragEventInit> {
    dataTransfer: DataTransfer;
}

interface SidebarProps {
    view: string;
    data: {
        components: Array<ComponentType>;
        flowControls: Array<FlowControlType>;
    };
}

const Item = ({data}: SidebarItemProps): JSX.Element => {
    const onDragStart = (event: DragEvent, label: string) => {
        event.dataTransfer.setData('application/reactflow', label);
        event.dataTransfer.effectAllowed = 'move';
    };

    return (
        <li
            className="my-1 flex h-[72px] items-center rounded-md bg-white p-2 hover:cursor-pointer hover:bg-gray-100"
            draggable
            id={data.display.label}
            onDragStart={(event) => onDragStart(event, data.display.label)}
        >
            <Component1Icon className="mr-2 h-7 w-7 flex-none " />

            <div className="flex flex-col">
                <p className="text-sm font-medium text-gray-900">
                    {data.display.label}
                </p>

                {/* eslint-disable-next-line tailwindcss/no-custom-classname */}
                <p className="text-left text-xs text-gray-500 line-clamp-2">
                    {data.display.description}
                </p>
            </div>
        </li>
    );
};

const LeftSidebar: React.FC<SidebarProps> = ({
    data,
    view = 'components',
}): JSX.Element => {
    return (
        <Provider>
            <div className="px-2">
                <ul role="list" className="mb-2">
                    {view === 'components'
                        ? data.components.map((component: ComponentType) => (
                              <Item key={component.name} data={component} />
                          ))
                        : data.flowControls.map(
                              (flowControl: FlowControlType) => (
                                  <Item
                                      key={flowControl.name}
                                      data={flowControl}
                                  />
                              )
                          )}
                </ul>
            </div>
        </Provider>
    );
};

export default LeftSidebar;
