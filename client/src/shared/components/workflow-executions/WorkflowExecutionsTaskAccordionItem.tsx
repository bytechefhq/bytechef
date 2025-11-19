import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {TaskTreeItemProps} from '@/shared/components/workflow-executions/WorkflowExecutionsUtils';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionsTaskAccordionItem = ({
    nestedItem,
    onTaskClick,
    selectedTaskExecutionId,
    taskTreeItem,
}: {
    nestedItem?: boolean;
    onTaskClick: (taskExecution: TaskExecution) => void;
    selectedTaskExecutionId: string;
    taskTreeItem: TaskTreeItemProps;
}) => {
    const hasChildren = taskTreeItem.children?.length > 0;
    const hasIterations = taskTreeItem.iterations?.length && taskTreeItem.iterations?.length > 0;

    return (
        <AccordionItem
            className={twMerge('border-b-0 pl-2', nestedItem && 'pl-4')}
            key={taskTreeItem.task.id}
            value={taskTreeItem.task.id || ''}
        >
            <AccordionTrigger
                className={twMerge(
                    'group flex w-full items-center justify-between rounded-md border border-stroke-neutral-primary p-2 hover:border-stroke-brand-primary hover:no-underline [&[data-state=open]]:border-stroke-brand-primary [&[data-state=open]]:hover:border-stroke-brand-secondary',
                    selectedTaskExecutionId === taskTreeItem.task.id &&
                        'border-stroke-brand-primary bg-surface-neutral-secondary hover:bg-surface-neutral-secondary [&[data-state=open]]:border-stroke-brand-primary',
                    !hasChildren &&
                        !hasIterations &&
                        '[&[data-state=closed]>svg]:hidden [&[data-state=open]>svg]:hidden'
                )}
                onClick={() => onTaskClick(taskTreeItem.task)}
            >
                <WorkflowTaskExecutionItem taskExecution={taskTreeItem.task} />
            </AccordionTrigger>

            {(hasChildren || hasIterations) && (
                <AccordionContent
                    className="border-l border-stroke-neutral-secondary p-0"
                    onClick={(event) => event.stopPropagation()}
                >
                    {hasIterations ? (
                        <Accordion className="mt-2 space-y-2" collapsible type="single">
                            {taskTreeItem.iterations?.map((iterationItems, index) => {
                                const iterationValue = `${taskTreeItem.task.id}-iteration-${index}`;

                                return (
                                    <AccordionItem
                                        className="border-b-0 pl-4"
                                        key={iterationValue}
                                        value={iterationValue}
                                    >
                                        <AccordionTrigger className="group flex w-full items-center justify-between rounded-md border border-stroke-neutral-primary p-4 hover:border-stroke-brand-primary hover:no-underline [&[data-state=open]]:border-stroke-brand-primary [&[data-state=open]]:hover:border-stroke-brand-secondary">
                                            <div className="flex w-full items-center justify-between">
                                                <span className="text-sm font-medium text-foreground">
                                                    Loop iteration {index + 1}
                                                </span>

                                                <span className="mr-2 text-xs text-muted-foreground">
                                                    {iterationItems.length} tasks
                                                </span>
                                            </div>
                                        </AccordionTrigger>

                                        {iterationItems.length > 0 && (
                                            <AccordionContent className="border-l border-stroke-neutral-secondary p-0">
                                                <Accordion className="mt-2 space-y-2" collapsible type="single">
                                                    {iterationItems.map((childItem) => (
                                                        <WorkflowExecutionsTaskAccordionItem
                                                            key={childItem.task.id}
                                                            nestedItem
                                                            onTaskClick={onTaskClick}
                                                            selectedTaskExecutionId={selectedTaskExecutionId || ''}
                                                            taskTreeItem={childItem}
                                                        />
                                                    ))}
                                                </Accordion>
                                            </AccordionContent>
                                        )}
                                    </AccordionItem>
                                );
                            })}
                        </Accordion>
                    ) : (
                        <Accordion className="mt-2 space-y-2" collapsible type="single">
                            {taskTreeItem.children.map((childItem) => (
                                <WorkflowExecutionsTaskAccordionItem
                                    key={childItem.task.id}
                                    nestedItem
                                    onTaskClick={onTaskClick}
                                    selectedTaskExecutionId={selectedTaskExecutionId || ''}
                                    taskTreeItem={childItem}
                                />
                            ))}
                        </Accordion>
                    )}
                </AccordionContent>
            )}
        </AccordionItem>
    );
};

export default WorkflowExecutionsTaskAccordionItem;
