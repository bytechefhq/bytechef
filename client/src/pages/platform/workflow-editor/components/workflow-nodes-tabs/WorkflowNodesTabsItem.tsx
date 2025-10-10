import {
    ClusterElementDefinitionBasic,
    ComponentDefinitionBasic,
    TaskDispatcherDefinition,
} from '@/shared/middleware/platform/configuration';
import {ComponentIcon} from 'lucide-react';
import {HTMLAttributes, MouseEvent, useEffect, useRef, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

interface DragEventI<T = Element> extends MouseEvent<T, DragEventInit> {
    dataTransfer: DataTransfer;
}

interface WorkflowNodesTabsItemProps extends HTMLAttributes<HTMLLIElement> {
    handleClick?: () => void;
    node: (ComponentDefinitionBasic | TaskDispatcherDefinition | ClusterElementDefinitionBasic) & {
        clusterElement?: boolean;
        taskDispatcher: boolean;
        trigger: boolean;
    };
    selected?: boolean;
}

const WorkflowNodesTabsItem = ({draggable, handleClick, node, selected}: WorkflowNodesTabsItemProps) => {
    const [isVisible, setIsVisible] = useState(false);
    const ref = useRef<HTMLLIElement>(null);

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

    useEffect(() => {
        const observer = new IntersectionObserver(
            ([entry]) => {
                if (entry.isIntersecting) {
                    setIsVisible(true);
                    observer.disconnect();
                }
            },
            {
                rootMargin: '50px',
                threshold: 0.1,
            }
        );

        if (ref.current) {
            observer.observe(ref.current);
        }

        return () => observer.disconnect();
    }, []);

    return (
        <li
            className={twMerge(
                'flex h-16 cursor-pointer items-center rounded-md border-2 border-transparent bg-white px-2 py-1 hover:border-blue-200',
                selected && 'border-blue-500 hover:bg-white'
            )}
            draggable={draggable}
            id={node?.title}
            onClick={(event) => {
                if (handleClick) {
                    handleClick();
                }

                event.stopPropagation();
            }}
            onDragStart={(event) => onDragStart(event)}
            ref={ref}
        >
            {node.icon ? (
                isVisible ? (
                    <InlineSVG
                        cacheRequests={true}
                        className="mr-2 size-7 flex-none"
                        loader={<ComponentIcon className="mr-2 size-7 flex-none" />}
                        src={node.icon}
                        title={node.title}
                    />
                ) : (
                    <ComponentIcon className="mr-2 size-7 flex-none" />
                )
            ) : (
                <ComponentIcon className="mr-2 size-7 flex-none" />
            )}

            <div className="flex flex-col">
                <p className="text-sm font-medium">{node?.title}</p>

                <p className="line-clamp-2 text-left text-xs text-muted-foreground">{node?.description}</p>
            </div>
        </li>
    );
};

export default WorkflowNodesTabsItem;
