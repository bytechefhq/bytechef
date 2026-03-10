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
    const iconRef = useRef<HTMLSpanElement>(null);

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

        if (iconRef.current) {
            const nodeContainer = document.createElement('div');

            nodeContainer.style.cssText =
                'width:56px;height:56px;border-radius:8px;border:2px solid #e2e8f0;background:#fff;display:flex;align-items:center;justify-content:center;position:absolute;top:-1000px;left:-1000px;';

            const iconClone = iconRef.current.cloneNode(true) as HTMLElement;

            iconClone.style.cssText = 'width:28px;height:28px;margin:0;';

            const svgElement = iconClone.querySelector('svg');

            if (svgElement) {
                svgElement.style.cssText = 'width:100%;height:100%;display:block;';
            }

            nodeContainer.appendChild(iconClone);
            document.body.appendChild(nodeContainer);
            event.dataTransfer.setDragImage(nodeContainer, 28, 28);
            setTimeout(() => document.body.removeChild(nodeContainer), 0);
        }
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
            <span className="mr-2 flex-none" ref={iconRef}>
                {node.icon ? (
                    isVisible ? (
                        <InlineSVG
                            cacheRequests={true}
                            className="size-7"
                            loader={<ComponentIcon className="size-7" />}
                            src={node.icon}
                            title={node.title}
                        />
                    ) : (
                        <ComponentIcon className="size-7" />
                    )
                ) : (
                    <ComponentIcon className="size-7" />
                )}
            </span>

            <div className="flex flex-col">
                <p className="text-sm font-medium">{node?.title}</p>

                <p className="line-clamp-2 text-left text-xs text-muted-foreground">{node?.description}</p>
            </div>
        </li>
    );
};

export default WorkflowNodesTabsItem;
