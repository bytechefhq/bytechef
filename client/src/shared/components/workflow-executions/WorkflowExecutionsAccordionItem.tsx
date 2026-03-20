import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {HoverCard, HoverCardContent, HoverCardTrigger} from '@/components/ui/hover-card';
import {ScrollArea, ScrollBar} from '@/components/ui/scroll-area';
import ExecutionAccordionButton from '@/shared/components/workflow-executions/ExecutionAccordionButton';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import {
    TaskExecution,
    TaskExecutionFromJSON,
    TriggerExecution,
} from '@/shared/middleware/automation/workflow/execution';
import {type ReactNode} from 'react';
import ReactJson from 'react-json-view';
import {twMerge} from 'tailwind-merge';

const isTaskExecution = (execution: TaskExecution | TriggerExecution): execution is TaskExecution =>
    'jobId' in execution;

interface WorkflowExecutionsAccordionItemProps {
    children: ReactNode;
    execution: TaskExecution | TriggerExecution;
    onExecutionClick: (execution: TaskExecution | TriggerExecution) => void;
    selectedExecutionId: string;
}

const WorkflowExecutionsAccordionItem = ({
    children,
    execution,
    onExecutionClick,
    selectedExecutionId,
}: WorkflowExecutionsAccordionItemProps) => {
    const taskExecution = isTaskExecution(execution) ? execution : undefined;

    const hasChildren = taskExecution?.children && taskExecution.children.length > 0;
    const hasIterations = taskExecution?.iterations && taskExecution.iterations.length > 0;

    const isExpandable = hasChildren || hasIterations;
    const isSelected = selectedExecutionId === execution.id;

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
                        'border-stroke-brand-primary bg-surface-neutral-secondary hover:bg-surface-neutral-secondary [&[data-state=open]]:border-stroke-brand-primary'
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
                {hasIterations ? (
                    <Accordion className="mt-2 space-y-2" type="multiple">
                        {taskExecution.iterations?.map((iteration, index) => {
                            const iterationValue = `${taskExecution.id}-iteration-${index}`;
                            const currentIterationItem = taskExecution.input?.items?.[index];
                            const convertedIterationItems = (iteration as unknown[]).map((item) =>
                                TaskExecutionFromJSON(item)
                            );

                            return (
                                <AccordionItem className="border-b-0" key={iterationValue} value={iterationValue}>
                                    <HoverCard openDelay={200}>
                                        <HoverCardTrigger className="[&[data-state=open]_button]:bg-surface-neutral-secondary [&[data-state=open]_span]:text-content-brand-primary [&[data-state=open]_svg]:text-content-brand-primary">
                                            <AccordionTrigger className="flex w-full min-w-0 items-center justify-between rounded-md border border-stroke-neutral-primary p-2 hover:border-stroke-brand-primary hover:no-underline focus-visible:outline-stroke-brand-focus focus-visible:transition-colors [&[data-state=closed]:hover>svg]:!rotate-0 [&[data-state=open]>svg]:!rotate-180 [&[data-state=open]]:hover:border-stroke-brand-secondary [&_svg]:size-5">
                                                <div className="flex w-full items-center justify-between">
                                                    <span className="text-sm font-medium text-content-neutral-primary">
                                                        {taskExecution.title || ''} iteration {index + 1}
                                                    </span>

                                                    <div className="mr-2 flex items-center gap-x-1 text-xs text-content-neutral-secondary">
                                                        <span>{convertedIterationItems.length}</span>

                                                        <span>
                                                            {convertedIterationItems.length > 1 ? 'tasks' : 'task'}
                                                        </span>
                                                    </div>
                                                </div>
                                            </AccordionTrigger>
                                        </HoverCardTrigger>

                                        {currentIterationItem != null && (
                                            <HoverCardContent
                                                align="start"
                                                className="flex max-h-56 w-fit min-w-40 max-w-sm flex-col rounded-md border border-stroke-neutral-primary bg-surface-neutral-secondary p-3 text-sm"
                                                side="right"
                                            >
                                                <span className="mb-1.5 shrink-0 text-sm font-semibold text-content-brand-primary">
                                                    {taskExecution.title || ''} item {index + 1}
                                                </span>

                                                <ScrollArea className="max-h-48 rounded">
                                                    <pre className="min-w-full bg-surface-neutral-primary p-4 text-xs text-content-neutral-primary">
                                                        {typeof currentIterationItem === 'object' &&
                                                        currentIterationItem !== null ? (
                                                            <ReactJson
                                                                enableClipboard={false}
                                                                src={currentIterationItem}
                                                            />
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
                                            <Accordion className="mt-2 space-y-2" type="multiple">
                                                {convertedIterationItems.map((convertedIterationItem) => (
                                                    <WorkflowExecutionsAccordionItem
                                                        execution={convertedIterationItem}
                                                        key={convertedIterationItem.id}
                                                        onExecutionClick={onExecutionClick}
                                                        selectedExecutionId={selectedExecutionId}
                                                    >
                                                        <WorkflowTaskExecutionItem
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
                    </Accordion>
                ) : (
                    <Accordion className="mt-2 space-y-2" type="multiple">
                        {taskExecution.children?.map((childTaskExecution) => (
                            <WorkflowExecutionsAccordionItem
                                execution={childTaskExecution}
                                key={childTaskExecution.id}
                                onExecutionClick={onExecutionClick}
                                selectedExecutionId={selectedExecutionId}
                            >
                                <WorkflowTaskExecutionItem taskExecution={childTaskExecution} />
                            </WorkflowExecutionsAccordionItem>
                        ))}
                    </Accordion>
                )}
            </AccordionContent>
        </AccordionItem>
    );
};

export default WorkflowExecutionsAccordionItem;
