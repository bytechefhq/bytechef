import {useEffect, useState} from 'react';
import {Provider} from '@radix-ui/react-tooltip';
import Button from 'components/Button/Button';
import cx from 'classnames';
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

const Item = ({data}: SidebarItemProps): JSX.Element => (
    <li className="mb-2 flex items-center rounded-lg bg-white p-2 text-center shadow-md hover:shadow">
        <Component1Icon className="mr-2 h-5 w-5 flex-none" />

        <div className="flex flex-col">
            <p className="font-medium text-gray-900">{data.display.label}</p>

            {/* eslint-disable-next-line tailwindcss/no-custom-classname */}
            <p className="text-left text-sm text-gray-500 line-clamp-2">
                {data.display.description}
            </p>
        </div>
    </li>
);

const LeftSidebar = (): JSX.Element => {
    const [components, setComponents] = useState([]);
    const [flowControls, setFlowControls] = useState([]);
    const [view, setView] = useState('components');

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
            <div className="h-full w-1/5 bg-slate-200 dark:bg-gray-800">
                <header className="flex justify-center space-x-4 p-4">
                    <div className="flex">
                        <Button
                            className={cx(
                                '!rounded-r-none !shadow-none',
                                view === 'components' && '!bg-gray-700'
                            )}
                            label="Components"
                            onClick={() => setView('components')}
                        />

                        <Button
                            className={cx(
                                '!rounded-l-none !shadow-none',
                                view === 'low-controls' && '!bg-gray-700'
                            )}
                            label="Flow Controls"
                            onClick={() => setView('flow-controls')}
                        />
                    </div>
                </header>

                <div className="flex h-full flex-col space-y-4 p-2">
                    <ul
                        role="list"
                        className="divide-y divide-gray-200 overflow-auto p-2"
                    >
                        {view === 'components'
                            ? components.map((component: ComponentType) => (
                                  <Item key={component.name} data={component} />
                              ))
                            : flowControls.map(
                                  (flowControl: FlowControlType) => (
                                      <Item
                                          key={flowControl.name}
                                          data={flowControl}
                                      />
                                  )
                              )}
                    </ul>
                </div>
            </div>
        </Provider>
    );
};

export default LeftSidebar;
