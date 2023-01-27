import React, {useEffect, useState} from 'react';
import {Provider} from '@radix-ui/react-tooltip';
import {Component1Icon} from '@radix-ui/react-icons';

interface ComponentType {
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
interface FlowControlType {
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

interface SidebarProps {
    view: string;
}

const Item = ({data}: SidebarItemProps): JSX.Element => (
    <li className="my-1 flex h-[72px] items-center rounded-md bg-white p-2 hover:cursor-pointer hover:bg-gray-100">
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

const LeftSidebar: React.FC<SidebarProps> = ({
    view = 'components',
}): JSX.Element => {
    const [components, setComponents] = useState([]);
    const [flowControls, setFlowControls] = useState([]);

    useEffect(() => {
        fetch('http://localhost:5173/api/definitions/components')
            .then((response) => response.json())
            .then((components) => setComponents(components));

        fetch('http://localhost:5173/api/definitions/task-dispatchers')
            .then((response) => response.json())
            .then((flowControls) => setFlowControls(flowControls));
    }, []);

    return (
        <Provider>
            <div className="px-2">
                <ul role="list" className="mb-2">
                    {view === 'components'
                        ? components.map((component: ComponentType) => (
                              <Item key={component.name} data={component} />
                          ))
                        : flowControls.map((flowControl: FlowControlType) => (
                              <Item key={flowControl.name} data={flowControl} />
                          ))}
                </ul>
            </div>
        </Provider>
    );
};

export default LeftSidebar;
