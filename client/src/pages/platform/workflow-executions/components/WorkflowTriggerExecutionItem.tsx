import WorkflowExecutionBadge from '@/pages/platform/workflow-executions/components/WorkflowExecutionBadge';
import {TriggerExecutionModel} from '@/shared/middleware/automation/workflow/execution';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const WorkflowTriggerExecutionItem = ({
    onClick,
    selected,
    triggerExecution,
}: {
    selected?: boolean;
    triggerExecution: TriggerExecutionModel;
    onClick?: () => void;
}) => {
    const {component, endDate, startDate, workflowTrigger} = triggerExecution;

    const duration = startDate && endDate && Math.round(endDate?.getTime() - startDate.getTime());

    return (
        <li
            className={twMerge(
                'flex w-full cursor-pointer items-center justify-between p-4 hover:bg-muted',
                selected && 'font-semibold bg-gray-50'
            )}
            onClick={() => onClick && onClick()}
        >
            <div className="flex items-center space-x-2 text-sm">
                <WorkflowExecutionBadge success={triggerExecution.status === 'COMPLETED'} />

                <div className="flex items-center gap-x-1">
                    {triggerExecution?.component?.icon && (
                        <InlineSVG className="size-4" src={triggerExecution?.component?.icon} />
                    )}

                    <span>{component?.title}</span>

                    <span className="text-xs text-muted-foreground">
                        ({workflowTrigger?.name || workflowTrigger?.type})
                    </span>
                </div>
            </div>

            <div className="flex items-center">
                <span className="ml-auto mr-2 text-xs">{duration ?? 0}ms</span>
            </div>
        </li>
    );
};

export default WorkflowTriggerExecutionItem;
