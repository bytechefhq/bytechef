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

            nodeContainer.style.position = 'absolute';
            nodeContainer.style.top = '-9999px';
            nodeContainer.style.left = '-9999px';
            nodeContainer.style.width = '56px';
            nodeContainer.style.height = '56px';
            nodeContainer.style.display = 'flex';
            nodeContainer.style.alignItems = 'center';
            nodeContainer.style.justifyContent = 'center';
            nodeContainer.style.backgroundColor = '#ffffff';
            nodeContainer.style.border = '2px solid #e2e8f0';
            nodeContainer.style.borderRadius = '8px';
            nodeContainer.style.zIndex = '-9999';

            const iconClone = iconRef.current.cloneNode(true) as HTMLElement;

            iconClone.style.margin = '0';
            iconClone.style.width = '28px';
            iconClone.style.height = '28px';
            iconClone.style.display = 'flex';
            iconClone.style.alignItems = 'center';
            iconClone.style.justifyContent = 'center';

            const svgElement = iconClone.querySelector('svg');

            if (svgElement) {
                svgElement.style.display = 'block';
                svgElement.style.width = '100%';
                svgElement.style.height = '100%';
            }

            nodeContainer.appendChild(iconClone);
            document.body.appendChild(nodeContainer);

            const dragX = nodeContainer.offsetWidth > 0 ? 28 : 28;
            const dragY = 28;

            event.dataTransfer.setDragImage(nodeContainer, dragX, dragY);

            requestAnimationFrame(() => {
                if (nodeContainer.parentNode) {
                    document.body.removeChild(nodeContainer);
                }
            });
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
