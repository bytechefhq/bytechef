import {Component1Icon} from '@radix-ui/react-icons';
import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionModel,
} from 'middleware/hermes/configuration';
import {HTMLAttributes, MouseEvent} from 'react';
import InlineSVG from 'react-inlinesvg';

interface DragEvent<T = Element> extends MouseEvent<T, DragEventInit> {
    dataTransfer: DataTransfer;
}

interface WorkflowNodesListItemProps extends HTMLAttributes<HTMLLIElement> {
    handleClick?: () => void;
    node: ComponentDefinitionBasicModel | TaskDispatcherDefinitionModel;
}

const WorkflowNodesListItem = ({
    draggable,
    handleClick,
    node,
}: WorkflowNodesListItemProps) => {
    const onDragStart = (event: DragEvent, name: string) => {
        event.dataTransfer.setData('application/reactflow', name);
        event.dataTransfer.effectAllowed = 'move';
    };

    return (
        <li
            className="mb-2 flex h-[72px] cursor-pointer items-center rounded-md bg-white p-2 hover:bg-gray-50"
            draggable={draggable}
            id={node?.title}
            onDragStart={(event) => onDragStart(event, node.name!)}
            onClick={handleClick}
        >
            {node.icon ? (
                <InlineSVG className="mr-2 h-7 w-7 flex-none" src={node.icon} />
            ) : (
                <Component1Icon className="mr-2 h-7 w-7 flex-none" />
            )}

            <div className="flex flex-col">
                <p className="text-sm font-medium text-gray-900">
                    {node?.title}
                </p>

                <p className="line-clamp-2 text-left text-xs text-gray-500">
                    {node?.description}
                </p>
            </div>
        </li>
    );
};

type WorkflowNodesListProps = {
    components: Array<ComponentDefinitionBasicModel>;
    flowControls: Array<TaskDispatcherDefinitionModel>;
    itemsDraggable?: boolean;
    onItemClick?: (
        clickedItem:
            | ComponentDefinitionBasicModel
            | TaskDispatcherDefinitionModel
    ) => void;
};

const WorkflowNodesList = ({
    components,
    flowControls,
    itemsDraggable = false,
    onItemClick,
}: WorkflowNodesListProps) => (
    <div className="px-2">
        <span className="sticky top-0 z-10 block w-full bg-gray-100 p-2 text-center text-sm font-bold uppercase text-gray-500">
            Components
        </span>

        <ul role="list" className="mb-2">
            {components.length ? (
                components.map((component: ComponentDefinitionBasicModel) => (
                    <WorkflowNodesListItem
                        draggable={itemsDraggable}
                        handleClick={() =>
                            onItemClick && onItemClick(component)
                        }
                        key={component.name}
                        node={component}
                    />
                ))
            ) : (
                <span className="block px-3 py-2 text-xs text-gray-500">
                    No components found.
                </span>
            )}
        </ul>

        <span className="sticky top-0 z-10 mt-2 block w-full bg-gray-100 p-2 text-center text-sm font-bold uppercase text-gray-500">
            Flow Controls
        </span>

        <ul role="list" className="mb-2">
            {flowControls.length ? (
                flowControls.map(
                    (flowControl: TaskDispatcherDefinitionModel) => (
                        <WorkflowNodesListItem
                            draggable={itemsDraggable}
                            handleClick={() =>
                                onItemClick && onItemClick(flowControl)
                            }
                            key={flowControl.name}
                            node={flowControl}
                        />
                    )
                )
            ) : (
                <span className="block px-3 py-2 text-xs text-gray-500">
                    No flow controls found.
                </span>
            )}
        </ul>
    </div>
);

export default WorkflowNodesList;
