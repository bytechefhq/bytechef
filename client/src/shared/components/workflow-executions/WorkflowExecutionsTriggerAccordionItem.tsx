import Button from '@/components/Button/Button';
import WorkflowTriggerExecutionItem from '@/shared/components/workflow-executions/WorkflowTriggerExecutionItem';
import {TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {twMerge} from 'tailwind-merge';

const WorkflowExecutionsTriggerAccordionItem = ({
    onTaskClick,
    selectedItem,
    triggerExecution,
}: {
    onTaskClick: (taskExecution: TaskExecution | TriggerExecution) => void;
    selectedItem: TaskExecution | TriggerExecution | undefined;
    triggerExecution: TriggerExecution;
}) => {
    const isSelected = selectedItem?.id === triggerExecution.id;

    return (
        <Button
            className={twMerge(
                'active:text-content-primary h-auto w-full justify-between rounded-md border border-stroke-neutral-primary p-2 text-left transition-colors hover:border-stroke-brand-primary hover:bg-transparent focus-visible:outline focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-stroke-brand-focus focus-visible:ring-0 focus-visible:transition-colors active:bg-transparent [&_svg]:size-5',
                isSelected &&
                    'border-stroke-brand-primary bg-surface-neutral-secondary hover:bg-surface-neutral-secondary active:bg-surface-neutral-secondary'
            )}
            onClick={() => {
                if (!isSelected) {
                    onTaskClick(triggerExecution);
                }
            }}
            type="button"
            variant="ghost"
        >
            <WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />
        </Button>
    );
};

export default WorkflowExecutionsTriggerAccordionItem;
