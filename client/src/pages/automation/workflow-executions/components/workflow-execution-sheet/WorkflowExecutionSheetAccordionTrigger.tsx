import {AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import WorkflowTriggerExecutionItem from '@/shared/components/workflow-executions/WorkflowTriggerExecutionItem';
import {TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionSheetAccordionTrigger = ({
    onTaskClick,
    selectedItem,
    triggerExecution,
}: {
    onTaskClick: (taskExecution: TaskExecution | TriggerExecution) => void;
    selectedItem: TaskExecution | TriggerExecution | undefined;
    triggerExecution: TriggerExecution;
}) => {
    return (
        <AccordionItem className="border-b-0 pl-2" value={triggerExecution.id || ''}>
            <AccordionTrigger
                className={twMerge(
                    'group flex w-full items-center justify-between rounded-md border border-stroke-neutral-primary p-2 hover:border-stroke-brand-primary hover:no-underline [&[data-state=closed]>svg]:hidden [&[data-state=open]>svg]:hidden [&[data-state=open]]:border-stroke-brand-primary [&[data-state=open]]:hover:border-stroke-brand-secondary',
                    selectedItem?.id === triggerExecution.id &&
                        'border-stroke-brand-primary bg-surface-neutral-secondary hover:bg-surface-neutral-secondary [&[data-state=open]]:border-stroke-brand-primary'
                )}
                onClick={() => onTaskClick(triggerExecution)}
            >
                <WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />
            </AccordionTrigger>
        </AccordionItem>
    );
};

export default WorkflowExecutionSheetAccordionTrigger;
