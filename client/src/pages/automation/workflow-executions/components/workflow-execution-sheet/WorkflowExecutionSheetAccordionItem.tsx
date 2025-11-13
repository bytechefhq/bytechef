import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {TaskTreeNodeI} from '@/shared/components/workflow-executions/WorkflowExecutionsUtils';
import WorkflowTaskExecutionItem from '@/shared/components/workflow-executions/WorkflowTaskExecutionItem';
import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionSheetAccordionItem = ({
    node,
    onTaskClick,
    selectedTaskExecutionId,
}: {
    node: TaskTreeNodeI;
    onTaskClick: (taskExecution: TaskExecution) => void;
    selectedTaskExecutionId: string;
}) => {
    return (
        <AccordionItem className="border-b-0 pl-2" key={node.task.id} value={node.task.id || ''}>
            <AccordionTrigger
                className={twMerge(
                    'group flex w-full items-center justify-between rounded-md border border-stroke-neutral-primary p-2 hover:border-stroke-brand-primary hover:no-underline [&[data-state=open]]:border-stroke-brand-primary [&[data-state=open]]:hover:border-stroke-brand-secondary',
                    selectedTaskExecutionId === node.task.id &&
                        'border-stroke-brand-primary bg-surface-neutral-secondary hover:bg-surface-neutral-secondary [&[data-state=open]]:border-stroke-brand-primary',
                    node.children.length === 0 && '[&[data-state=closed]>svg]:hidden [&[data-state=open]>svg]:hidden'
                )}
                onClick={() => onTaskClick(node.task)}
            >
                <WorkflowTaskExecutionItem taskExecution={node.task} />
            </AccordionTrigger>

            {node.children.length > 0 && (
                <AccordionContent
                    className="border-l border-l-border/50 p-0"
                    onClick={(event) => event.stopPropagation()}
                >
                    <Accordion className="mt-2 space-y-2" collapsible type="single">
                        {node.children.map((childNode) => (
                            <WorkflowExecutionSheetAccordionItem
                                key={childNode.task.id}
                                node={childNode}
                                onTaskClick={onTaskClick}
                                selectedTaskExecutionId={selectedTaskExecutionId || ''}
                            />
                        ))}
                    </Accordion>
                </AccordionContent>
            )}
        </AccordionItem>
    );
};

export default WorkflowExecutionSheetAccordionItem;
