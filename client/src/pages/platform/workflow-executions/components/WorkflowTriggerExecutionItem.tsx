import WorkflowExecutionBadge from '@/pages/platform/workflow-executions/components/WorkflowExecutionBadge';
import {TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const WorkflowTriggerExecutionItem = ({
    onClick,
    selected,
    triggerExecution,
}: {
    selected?: boolean;
    triggerExecution: TriggerExecution;
    onClick?: () => void;
}) => {
    const {endDate, icon, startDate, title, workflowTrigger} = triggerExecution;

    const duration = startDate && endDate && Math.round(endDate?.getTime() - startDate.getTime());

    return (
        <li
            className={twMerge(
                'flex w-full cursor-pointer items-center justify-between p-4 hover:bg-muted',
                selected && 'bg-muted/50 font-semibold'
            )}
            onClick={() => onClick && onClick()}
        >
            <div className="flex items-center space-x-2 text-sm">
                <WorkflowExecutionBadge status={triggerExecution.status} />

                <div className="flex items-center gap-x-1">
                    {icon && <InlineSVG className="size-4" src={icon} />}

                    <span>{title}</span>

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
