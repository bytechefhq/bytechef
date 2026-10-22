import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {HoverCard, HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import {ScrollArea, ScrollBar} from '@/components/ui/scroll-area';
import JsonView from '@/shared/components/JsonView';
import {type LazyTaskExecutionChildrenOptionsI} from '@/shared/components/workflow-executions/LazyTaskExecutionChildren';
import WorkflowExecutionsAccordionItem from '@/shared/components/workflow-executions/WorkflowExecutionsAccordionItem';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import {
    Job,
    TaskExecution,
    TaskExecutionFromJSON,
    TriggerExecution,
} from '@/shared/middleware/automation/workflow/execution';
import {useEffect, useState} from 'react';

const ON_ERROR_BRANCH_LABELS = ['Try branch', 'Error branch'];

const isOnErrorDispatcher = (type?: string) => type?.toLowerCase().includes('on-error') ?? false;

const getIterationLabel = (taskExecution: TaskExecution, index: number) => {
    if (isOnErrorDispatcher(taskExecution.type)) {
        return ON_ERROR_BRANCH_LABELS[index] ?? `Branch ${index + 1}`;
    }

    return `${taskExecution.title || ''} iteration ${index + 1}`;
};

interface TaskExecutionChildrenListProps {
    children?: TaskExecution[];
    defaultValue?: string[];
    hasMore: boolean;
    indexOffset?: number;
    iterations?: TaskExecution[][];
    jobStatus?: Job['status'];
    lazy?: LazyTaskExecutionChildrenOptionsI;
    onExecutionClick: (execution: TaskExecution | TriggerExecution) => void;
    onLoadMore: () => void;
    parentTaskExecution: TaskExecution;
    remainingCount?: number;
    selectedExecutionId: string;
}

const TaskExecutionChildrenList = ({
    children,
    defaultValue,
    hasMore,
    indexOffset = 0,
    iterations,
    jobStatus,
    lazy,
    onExecutionClick,
    onLoadMore,
    parentTaskExecution,
    remainingCount = 0,
    selectedExecutionId,
}: TaskExecutionChildrenListProps) => {
    const [loadMoreElement, setLoadMoreElement] = useState<HTMLDivElement | null>(null);

    const hasIterations = iterations !== undefined && iterations.length > 0;
    const hasChildren = children !== undefined && children.length > 0;
    const loadedCount = hasIterations ? iterations.length : children?.length || 0;

    useEffect(() => {
        if (!loadMoreElement || !hasMore) {
            return;
        }

        const observer = new IntersectionObserver(
            (entries) => {
                if (entries[0]?.isIntersecting) {
                    onLoadMore();
                }
            },
            {root: loadMoreElement.closest('[data-radix-scroll-area-viewport]'), rootMargin: '200px'}
        );

        observer.observe(loadMoreElement);

        return () => observer.disconnect();
    }, [hasMore, loadMoreElement, loadedCount, onLoadMore]);

    if (hasIterations) {
        return (
            <Accordion className="mt-2 space-y-2" defaultValue={defaultValue} type="multiple">
                {iterations
                    .map((iteration, position) => ({index: indexOffset + position, iteration}))
                    .filter(({iteration}) => !isOnErrorDispatcher(parentTaskExecution.type) || iteration.length > 0)
                    .map(({index, iteration}) => {
                        const iterationValue = `${parentTaskExecution.id}-iteration-${index}`;
                        const currentIterationItem = parentTaskExecution.input?.items?.[index];
                        const convertedIterationItems = (iteration as unknown[]).map((item) =>
                            TaskExecutionFromJSON(item)
                        );
                        const iterationLabel = getIterationLabel(parentTaskExecution, index);

                        return (
                            <AccordionItem className="border-b-0" key={iterationValue} value={iterationValue}>
                                <HoverCard openDelay={200}>
                                    <HoverCardTrigger className="[&[data-state=open]_button]:bg-surface-neutral-secondary [&[data-state=open]_span]:text-content-brand-primary [&[data-state=open]_svg]:text-content-brand-primary">
                                        <AccordionTrigger className="flex w-full min-w-0 items-center justify-between rounded-md border border-stroke-neutral-primary p-2 hover:border-stroke-brand-primary hover:no-underline focus-visible:outline-stroke-brand-focus focus-visible:transition-colors data-[state=open]:hover:border-stroke-brand-secondary [&_svg]:size-5 [&[data-state=closed]:hover>svg]:rotate-0! [&[data-state=open]>svg]:rotate-180!">
                                            <div className="flex w-full items-center justify-between">
                                                <span className="text-sm font-medium text-content-neutral-primary">
                                                    {iterationLabel}
                                                </span>

                                                <div className="mr-2 flex items-center gap-x-1 text-xs text-content-neutral-secondary">
                                                    <span>{convertedIterationItems.length}</span>

                                                    <span>{convertedIterationItems.length > 1 ? 'tasks' : 'task'}</span>
                                                </div>
                                            </div>
                                        </AccordionTrigger>
                                    </HoverCardTrigger>

                                    {currentIterationItem != null && (
                                        <HoverCardContent
                                            align="start"
                                            className="flex max-h-56 w-fit max-w-sm min-w-40 flex-col rounded-md border border-stroke-neutral-primary bg-surface-neutral-secondary p-3 text-sm"
                                            hideWhenDetached
                                            side="right"
                                        >
                                            <span className="mb-1.5 shrink-0 text-sm font-semibold text-content-brand-primary">
                                                {parentTaskExecution.title || ''} item {index + 1}
                                            </span>

                                            <ScrollArea className="max-h-48 rounded">
                                                <pre className="min-w-full bg-surface-neutral-primary p-4 text-xs text-content-neutral-primary">
                                                    {typeof currentIterationItem === 'object' &&
                                                    currentIterationItem !== null ? (
                                                        <JsonView src={currentIterationItem as object} />
                                                    ) : (
                                                        String(currentIterationItem)
                                                    )}
                                                </pre>

                                                <ScrollBar orientation="horizontal" />

                                                <ScrollBar orientation="vertical" />
                                            </ScrollArea>
                                        </HoverCardContent>
                                    )}
                                </HoverCard>

                                {convertedIterationItems.length > 0 && (
                                    <AccordionContent className="border-l border-stroke-neutral-secondary p-0 pl-4">
                                        <Accordion
                                            className="mt-2 space-y-2"
                                            defaultValue={defaultValue}
                                            type="multiple"
                                        >
                                            {convertedIterationItems.map((convertedIterationItem) => (
                                                <WorkflowExecutionsAccordionItem
                                                    defaultValue={defaultValue}
                                                    execution={convertedIterationItem}
                                                    jobStatus={jobStatus}
                                                    key={convertedIterationItem.id}
                                                    lazy={lazy}
                                                    onExecutionClick={onExecutionClick}
                                                    selectedExecutionId={selectedExecutionId}
                                                >
                                                    <WorkflowTaskExecutionItem
                                                        jobStatus={jobStatus}
                                                        taskExecution={convertedIterationItem}
                                                    />
                                                </WorkflowExecutionsAccordionItem>
                                            ))}
                                        </Accordion>
                                    </AccordionContent>
                                )}
                            </AccordionItem>
                        );
                    })}

                {hasMore && (
                    <div
                        className="p-2 text-center text-sm text-content-neutral-secondary"
                        ref={setLoadMoreElement}
                        role="status"
                    >
                        {`Loading ${remainingCount} more iterations...`}
                    </div>
                )}
            </Accordion>
        );
    }

    if (!hasChildren) {
        return null;
    }

    return (
        <Accordion className="mt-2 space-y-2" defaultValue={defaultValue} type="multiple">
            {children.map((childTaskExecution) => (
                <WorkflowExecutionsAccordionItem
                    defaultValue={defaultValue}
                    execution={childTaskExecution}
                    jobStatus={jobStatus}
                    key={childTaskExecution.id}
                    lazy={lazy}
                    onExecutionClick={onExecutionClick}
                    selectedExecutionId={selectedExecutionId}
                >
                    <WorkflowTaskExecutionItem jobStatus={jobStatus} taskExecution={childTaskExecution} />
                </WorkflowExecutionsAccordionItem>
            ))}

            {hasMore && (
                <div
                    className="p-2 text-center text-sm text-content-neutral-secondary"
                    ref={setLoadMoreElement}
                    role="status"
                >
                    {`Loading ${remainingCount} more tasks...`}
                </div>
            )}
        </Accordion>
    );
};

export default TaskExecutionChildrenList;
