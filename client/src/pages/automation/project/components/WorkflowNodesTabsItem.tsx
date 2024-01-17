import {Component1Icon} from '@radix-ui/react-icons';
import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from 'middleware/platform/configuration';
import {HTMLAttributes, MouseEvent} from 'react';
import InlineSVG from 'react-inlinesvg';

interface DragEvent<T = Element> extends MouseEvent<T, DragEventInit> {
    dataTransfer: DataTransfer;
}

type WorkflowNodesTabsItemProps = {
    handleClick?: () => void;
    node: ComponentDefinitionBasicModel | TaskDispatcherDefinitionModel;
} & HTMLAttributes<HTMLLIElement>;

const WorkflowNodesTabsItem = ({draggable, handleClick, node}: WorkflowNodesTabsItemProps) => {
    const onDragStart = (event: DragEvent, name: string) => {
        event.dataTransfer.setData('application/reactflow', name);
        event.dataTransfer.effectAllowed = 'move';
    };

    return (
        <li
            className="mb-2 flex h-[72px] cursor-pointer items-center rounded-md bg-white p-2 hover:bg-gray-50"
            draggable={draggable}
            id={node?.title}
            onClick={handleClick}
            onDragStart={(event) => onDragStart(event, node.name!)}
        >
            {node.icon ? (
                <InlineSVG
                    className="mr-2 size-7 flex-none"
                    loader={<Component1Icon className="mr-2 size-7 flex-none" />}
                    src={node.icon}
                    title={node.title}
                />
            ) : (
                <Component1Icon className="mr-2 size-7 flex-none" />
            )}

            <div className="flex flex-col">
                <p className="text-sm font-medium">{node?.title}</p>

                <p className="line-clamp-2 text-left text-xs text-gray-500">{node?.description}</p>
            </div>
        </li>
    );
};

export default WorkflowNodesTabsItem;
