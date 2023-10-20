import React from 'react';
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
    view: string;
    data: {
        components: Array<ComponentDefinitionModel>;
        flowControls: Array<TaskDispatcherDefinitionModel>;
    };
}

const Item = ({description, label}: DisplayModel): JSX.Element => {
    const onDragStart = (event: DragEvent, label: string) => {
        event.dataTransfer.setData('application/reactflow', label);
        event.dataTransfer.effectAllowed = 'move';
    };

    return (
        <li
            className="my-1 flex h-[72px] items-center rounded-md bg-white p-2 hover:cursor-pointer hover:bg-gray-100"
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
    view = 'components',
}): JSX.Element => (
    <Provider>
        <div className="px-2">
            <ul role="list" className="mb-2">
                {view === 'components'
                    ? data.components.map(
                          (component: ComponentDefinitionModel) => (
                              <Item
                                  description={component.display?.description}
                                  key={component.name}
                                  label={component.display?.label}
                              />
                          )
                      )
                    : data.flowControls.map(
                          (flowControl: TaskDispatcherDefinitionModel) => (
                              <Item
                                  description={flowControl.display?.description}
                                  key={flowControl.name}
                                  label={flowControl.display?.label}
                              />
                          )
                      )}
            </ul>
        </div>
    </Provider>
);

export default LeftSidebar;
