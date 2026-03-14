import {
    ClusterElementDefinitionBasic,
    ComponentDefinitionBasic,
    TaskDispatcherDefinition,
} from '@/shared/middleware/platform/configuration';
import { ComponentIcon } from 'lucide-react';
import { HTMLAttributes, MouseEvent, useEffect, useRef, useState } from 'react';
import InlineSVG from 'react-inlinesvg';
import { twMerge } from 'tailwind-merge';


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
    draggable?: boolean;
}

const WorkflowNodesTabsItem = ({
    draggable,
    handleClick,
    node,
    selected,
}: WorkflowNodesTabsItemProps) => {
    const [isVisible, setIsVisible] = useState(false);
    const ref = useRef<HTMLLIElement>(null);

    
    let nodeName = node.name;
    if (node.trigger) nodeName = `${node.name}--trigger`;
    if (node.taskDispatcher) nodeName = `${node.name}--taskDispatcher`;

   
    const onDragStart = (event: DragEventI) => {
        event.dataTransfer.setData('application/reactflow', nodeName);
        event.dataTransfer.effectAllowed = 'move';

        const dragIcon = document.createElement('img');

        if (node.icon) {
            dragIcon.src = node.icon;
        } else {
            
            dragIcon.src =
                'data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiBoZWlnaHQ9IjI0IiBzdHJva2U9IiM4ODgiIHN0cm9rZS13aWR0aD0iMS41IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cGF0aCBkPSJNMTIgNWMxLjM3IDAgMi41Ljg5IDIuODYgMi4xOGwxLjA0LTIuMDhjLS40Mi0uMTItLjg3LS4xOC0xLjMuMThBNC41IDQuNSAwIDAgMCAxMiA0Yy0yLjQ4IDAtNCAxLjUyLTQgMy41IDAgMi4zOCAxLjk1IDQuNS00LjUgNC41UzguNSA5Ljg4IDggOC41YTMuNSA0LjUgMCAwIDEgMy4wMi0zLjM1bC0uNzMtMS45OEExMiAxMiAwIDEgMSAxMiA1WiIvPjwvc3ZnPg==';
        }

        
        dragIcon.style.width = '32px';
        dragIcon.style.height = '32px';
        dragIcon.style.position = 'absolute';
        dragIcon.style.top = '-1000px';
        dragIcon.style.left = '-1000px';
        document.body.appendChild(dragIcon);

        
        event.dataTransfer.setDragImage(dragIcon, 16, 16);

        
        setTimeout(() => {
            document.body.removeChild(dragIcon);
        }, 0);
    };

    
    useEffect(() => {
        const observer = new IntersectionObserver(
            ([entry]) => {
                if (entry.isIntersecting) {
                    setIsVisible(true);
                    observer.disconnect();
                }
            },
            { rootMargin: '50px', threshold: 0.1 }
        );

        if (ref.current) observer.observe(ref.current);
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
            ref={ref}
            onClick={(event) => {
                event.stopPropagation();
                handleClick?.();
            }}
            onDragStart={(event) => onDragStart(event)}
        >
            {node.icon ? (
                isVisible ? (
                    <InlineSVG
                        cacheRequests
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
