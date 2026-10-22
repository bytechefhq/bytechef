import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import ExecutionAccordionButton from '@/shared/components/workflow-executions/ExecutionAccordionButton';
import LazyTaskExecutionChildren, {
    type LazyTaskExecutionChildrenOptionsI,
} from '@/shared/components/workflow-executions/LazyTaskExecutionChildren';
import TaskExecutionChildrenList from '@/shared/components/workflow-executions/TaskExecutionChildrenList';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {type ReactNode, useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

const ITERATIONS_PAGE_SIZE = 100;

const isTaskExecution = (execution: TaskExecution | TriggerExecution): execution is TaskExecution =>
    'jobId' in execution;

interface WorkflowExecutionsAccordionItemProps {
    children: ReactNode;
    defaultValue?: string[];
    execution: TaskExecution | TriggerExecution;
    jobStatus?: Job['status'];
    lazy?: LazyTaskExecutionChildrenOptionsI;
    onExecutionClick: (execution: TaskExecution | TriggerExecution) => void;
    previousTaskExecutions?: TaskExecution[];
    selectedExecutionId: string;
}

const WorkflowExecutionsAccordionItem = ({
    children,
    defaultValue,
    execution,
    jobStatus,
    lazy,
    onExecutionClick,
    previousTaskExecutions = [],
    selectedExecutionId,
}: WorkflowExecutionsAccordionItemProps) => {
    const [visibleIterationCount, setVisibleIterationCount] = useState(ITERATIONS_PAGE_SIZE);

    const taskExecution = isTaskExecution(execution) ? execution : undefined;

    const hasChildren = taskExecution?.children && taskExecution.children.length > 0;
    const hasIterations = taskExecution?.iterations && taskExecution.iterations.length > 0;
    const remainingIterationCount = (taskExecution?.iterations?.length ?? 0) - visibleIterationCount;
    const hasMoreIterations = remainingIterationCount > 0;
    const hasPreviousTaskExecutions = previousTaskExecutions.length > 0;

    const hasLazyChildren = (taskExecution?.childCount ?? 0) > 0;

    const isExpandable = lazy
        ? hasLazyChildren || hasPreviousTaskExecutions
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

    if (!isExpandable) {
        return (
            <ExecutionAccordionButton
                isSelected={isSelected}
                onClick={() => {
                    if (!isSelected) {
                        onExecutionClick(execution);
                    }
                }}
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
            >
                {children}
            </AccordionTrigger>

            <AccordionContent
                className="border-l border-stroke-neutral-secondary p-0 pl-4"
                onClick={(event) => event.stopPropagation()}
            >
                {lazy ? (
                    hasLazyChildren && (
                        <LazyTaskExecutionChildren
                            defaultValue={defaultValue}
                            jobStatus={jobStatus}
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
                        jobStatus={jobStatus}
                        onExecutionClick={onExecutionClick}
                        onLoadMore={handleLoadMoreIterations}
                        parentTaskExecution={taskExecution}
                        remainingCount={remainingIterationCount}
                        selectedExecutionId={selectedExecutionId}
                    >
                        {taskExecution.children}
                    </TaskExecutionChildrenList>
                )}

                {hasPreviousTaskExecutions && (
                    <Accordion className="mt-2 space-y-2" defaultValue={defaultValue} type="multiple">
                        {previousTaskExecutions.map((previousTaskExecution, index) => (
                            <WorkflowExecutionsAccordionItem
                                defaultValue={defaultValue}
                                execution={previousTaskExecution}
                                jobStatus={jobStatus}
                                key={previousTaskExecution.id}
                                lazy={lazy}
                                onExecutionClick={onExecutionClick}
                                selectedExecutionId={selectedExecutionId}
                            >
                                <WorkflowTaskExecutionItem
                                    attemptLabel={`Attempt ${index + 1}`}
                                    jobStatus={jobStatus}
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
