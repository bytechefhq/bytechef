import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {Component1Icon} from '@radix-ui/react-icons';
import {HTMLAttributes, MouseEvent} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

interface DragEventI<T = Element> extends MouseEvent<T, DragEventInit> {
    dataTransfer: DataTransfer;
}

interface WorkflowNodesTabsItemProps extends HTMLAttributes<HTMLLIElement> {
    handleClick?: () => void;
    node: (ComponentDefinitionBasic | TaskDispatcherDefinition) & {taskDispatcher: boolean; trigger: boolean};
    selected?: boolean;
}

const WorkflowNodesTabsItem = ({draggable, handleClick, node, selected}: WorkflowNodesTabsItemProps) => {
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
            className={twMerge(
                'flex h-16 cursor-pointer items-center border-2 border-transparent rounded-md bg-white px-2 py-1 hover:border-blue-200',
                selected && 'border-blue-500 hover:bg-white'
            )}
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

                <p className="line-clamp-2 text-left text-xs text-muted-foreground">{node?.description}</p>
            </div>
        </li>
    );
};

export default WorkflowNodesTabsItem;
