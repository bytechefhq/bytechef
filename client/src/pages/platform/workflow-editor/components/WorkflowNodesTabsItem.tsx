import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionModel} from '@/shared/middleware/platform/configuration';
import {Component1Icon} from '@radix-ui/react-icons';
import {HTMLAttributes, MouseEvent} from 'react';
import InlineSVG from 'react-inlinesvg';

interface DragEventI<T = Element> extends MouseEvent<T, DragEventInit> {
    dataTransfer: DataTransfer;
}

interface WorkflowNodesTabsItemProps extends HTMLAttributes<HTMLLIElement> {
    handleClick?: () => void;
    node: (ComponentDefinitionBasicModel | TaskDispatcherDefinitionModel) & {taskDispatcher: boolean; trigger: boolean};
}

const WorkflowNodesTabsItem = ({draggable, handleClick, node}: WorkflowNodesTabsItemProps) => {
    let nodeName = node.name;

    if (node.trigger) {
        nodeName = `${node.name}--trigger`;
    }

    if (node.taskDispatcher) {
        nodeName = `${node.name}--taskDispatcher'`;
    }

    const onDragStart = (event: DragEventI) => {
        event.dataTransfer.setData('application/reactflow', nodeName);
        event.dataTransfer.effectAllowed = 'move';
    };

    return (
        <li
            className="flex h-[72px] cursor-pointer items-center rounded-md bg-white p-2 hover:bg-gray-50"
            draggable={draggable}
            id={node?.title}
            onClick={handleClick}
            onDragStart={(event) => onDragStart(event)}
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
