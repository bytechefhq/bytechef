import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import ExecutionAccordionButton from '@/shared/components/workflow-executions/ExecutionAccordionButton';
import LazyTaskExecutionChildren, {
    type LazyTaskExecutionChildrenOptionsI,
} from '@/shared/components/workflow-executions/LazyTaskExecutionChildren';
import TaskExecutionChildrenList from '@/shared/components/workflow-executions/TaskExecutionChildrenList';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import toClusterElementCallTaskExecutions from '@/shared/components/workflow-executions/util/toClusterElementCallTaskExecutions';
import {TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {TaskExecution as PlatformTaskExecution} from '@/shared/middleware/platform/workflow/execution';
import {type ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const ITERATIONS_PAGE_SIZE = 100;

const isTaskExecution = (execution: TaskExecution | TriggerExecution): execution is TaskExecution =>
    'jobId' in execution;

const scrollIntoScrollAreaView = (element: HTMLElement, viewport: Element | null) => {
    if (!viewport) {
        element.scrollIntoView({block: 'nearest'});

        return;
    }

    const elementRect = element.getBoundingClientRect();
    const viewportRect = viewport.getBoundingClientRect();

    if (elementRect.top < viewportRect.top) {
        viewport.scrollTop += elementRect.top - viewportRect.top;
    } else if (elementRect.bottom > viewportRect.bottom) {
        viewport.scrollTop += Math.min(elementRect.bottom - viewportRect.bottom, elementRect.top - viewportRect.top);
    }
};

interface WorkflowExecutionsAccordionItemProps {
    children: ReactNode;
    defaultValue?: string[];
    execution: TaskExecution | TriggerExecution;
    jobEnded?: boolean;
    lazy?: LazyTaskExecutionChildrenOptionsI;
    onExecutionClick: (execution: TaskExecution | TriggerExecution) => void;
    previousTaskExecutions?: TaskExecution[];
    selectedExecutionId: string;
}

const WorkflowExecutionsAccordionItem = ({
    children,
    defaultValue,
    execution,
    jobEnded,
    lazy,
    onExecutionClick,
    previousTaskExecutions = [],
    selectedExecutionId,
}: WorkflowExecutionsAccordionItemProps) => {
    const [visibleIterationCount, setVisibleIterationCount] = useState(ITERATIONS_PAGE_SIZE);

    const buttonRef = useRef<HTMLButtonElement>(null);

    const taskExecution = isTaskExecution(execution) ? execution : undefined;

    const childExecutions: TaskExecution[] = useMemo(() => {
        if (!taskExecution) {
            return [];
        }

        if (taskExecution.children && taskExecution.children.length > 0) {
            return taskExecution.children;
        }

        return toClusterElementCallTaskExecutions(taskExecution as PlatformTaskExecution) as TaskExecution[];
    }, [taskExecution]);

    const hasChildren = childExecutions.length > 0;
    const hasIterations = taskExecution?.iterations && taskExecution.iterations.length > 0;
    const remainingIterationCount = (taskExecution?.iterations?.length ?? 0) - visibleIterationCount;
    const hasMoreIterations = remainingIterationCount > 0;
    const hasPreviousTaskExecutions = previousTaskExecutions.length > 0;

    const hasLazyChildren = (taskExecution?.childCount ?? 0) > 0;
    const hasClusterElementCallChildren = !hasLazyChildren && hasChildren;

    const isExpandable = lazy
        ? hasLazyChildren || hasClusterElementCallChildren || hasPreviousTaskExecutions
        : hasChildren || hasIterations || hasPreviousTaskExecutions;
    const isSelected = selectedExecutionId === execution.id;

    const visibleIterations = useMemo(
        () => taskExecution?.iterations?.slice(0, visibleIterationCount),
        [taskExecution?.iterations, visibleIterationCount]
    );

    const handleLoadMoreIterations = useCallback(
        () => setVisibleIterationCount((count) => count + ITERATIONS_PAGE_SIZE),
        []
    );

    useEffect(() => {
        const button = buttonRef.current;

        if (!isSelected || !button) {
            return;
        }

        const viewport = button.closest('[data-radix-scroll-area-viewport]');

        scrollIntoScrollAreaView(button, viewport);

        const runningAnimations = (viewport?.getAnimations?.({subtree: true}) || []).filter(
            (animation) => animation.effect?.getComputedTiming().endTime !== Infinity
        );

        if (runningAnimations.length === 0) {
            return;
        }

        let cancelled = false;

        void Promise.allSettled(runningAnimations.map((animation) => animation.finished)).then(() => {
            if (!cancelled) {
                scrollIntoScrollAreaView(button, viewport);
            }
        });

        return () => {
            cancelled = true;
        };
    }, [isSelected]);

    if (!isExpandable) {
        return (
            <ExecutionAccordionButton
                isSelected={isSelected}
                onClick={() => {
                    if (!isSelected) {
                        onExecutionClick(execution);
                    }
                }}
                ref={buttonRef}
            >
                {children}
            </ExecutionAccordionButton>
        );
    }

    if (!taskExecution) {
        return;
    }

    return (
        <AccordionItem className="border-b-0" key={execution.id} value={execution.id || ''}>
            <AccordionTrigger
                className={twMerge(
                    'group flex w-full items-center justify-between rounded-md border border-stroke-neutral-primary p-2 hover:border-stroke-brand-primary hover:no-underline focus-visible:outline-stroke-brand-focus focus-visible:transition-colors [&_svg]:size-5',
                    isSelected &&
                        'border-stroke-brand-primary bg-surface-neutral-secondary hover:bg-surface-neutral-secondary data-[state=open]:border-stroke-brand-primary'
                )}
                onClick={() => {
                    if (!isSelected) {
                        onExecutionClick(execution);
                    }
                }}
                ref={buttonRef}
            >
                {children}
            </AccordionTrigger>

            <AccordionContent
                className="border-l border-stroke-neutral-secondary p-0 pl-4"
                onClick={(event) => event.stopPropagation()}
            >
                {lazy && !hasClusterElementCallChildren ? (
                    hasLazyChildren && (
                        <LazyTaskExecutionChildren
                            defaultValue={defaultValue}
                            jobEnded={jobEnded}
                            lazy={lazy}
                            onExecutionClick={onExecutionClick}
                            parentTaskExecution={taskExecution}
                            selectedExecutionId={selectedExecutionId}
                        />
                    )
                ) : (
                    <TaskExecutionChildrenList
                        defaultValue={defaultValue}
                        hasMore={hasMoreIterations}
                        iterations={visibleIterations}
                        jobEnded={jobEnded}
                        onExecutionClick={onExecutionClick}
                        onLoadMore={handleLoadMoreIterations}
                        parentTaskExecution={taskExecution}
                        remainingCount={remainingIterationCount}
                        selectedExecutionId={selectedExecutionId}
                    >
                        {childExecutions}
                    </TaskExecutionChildrenList>
                )}

                {hasPreviousTaskExecutions && (
                    <Accordion className="mt-2 space-y-2" defaultValue={defaultValue} type="multiple">
                        {previousTaskExecutions.map((previousTaskExecution, index) => (
                            <WorkflowExecutionsAccordionItem
                                defaultValue={defaultValue}
                                execution={previousTaskExecution}
                                jobEnded={jobEnded}
                                key={previousTaskExecution.id}
                                lazy={lazy}
                                onExecutionClick={onExecutionClick}
                                selectedExecutionId={selectedExecutionId}
                            >
                                <WorkflowTaskExecutionItem
                                    attemptLabel={`Attempt ${index + 1}`}
                                    jobEnded={jobEnded}
                                    taskExecution={previousTaskExecution}
                                />
                            </WorkflowExecutionsAccordionItem>
                        ))}
                    </Accordion>
                )}
            </AccordionContent>
        </AccordionItem>
    );
};

export default WorkflowExecutionsAccordionItem;
