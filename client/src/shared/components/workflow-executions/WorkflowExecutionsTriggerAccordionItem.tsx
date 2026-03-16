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
        <div className="pl-2">
            <button
                className={twMerge(
                    'group flex w-full items-center justify-between rounded-md border border-stroke-neutral-primary p-2 text-left hover:border-stroke-brand-primary focus-visible:outline-stroke-brand-focus focus-visible:transition-colors',
                    isSelected &&
                        'border-stroke-brand-primary bg-surface-neutral-secondary hover:bg-surface-neutral-secondary'
                )}
                onClick={() => {
                    if (!isSelected) {
                        onTaskClick(triggerExecution);
                    }
                }}
            >
                <WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />
            </button>
        </div>
    );
};

export default WorkflowExecutionsTriggerAccordionItem;
