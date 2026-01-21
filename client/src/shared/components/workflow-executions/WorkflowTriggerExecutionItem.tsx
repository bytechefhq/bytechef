import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import {getExecutionStatusIcon} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {TriggerExecution} from '@/shared/middleware/automation/workflow/execution';

const WorkflowTriggerExecutionItem = ({triggerExecution}: {triggerExecution: TriggerExecution}) => {
    const {endDate, icon, startDate, status, title, workflowTrigger} = triggerExecution;

    const duration = startDate && endDate && Math.round(endDate?.getTime() - startDate.getTime());
    const statusIcon = getExecutionStatusIcon(status);

    return (
        <li className="flex w-full cursor-pointer items-center justify-between rounded-lg p-0 hover:bg-inherit">
            <div className="flex items-center gap-x-2 text-sm">
                {statusIcon}

                <div className="flex items-center gap-x-1">
                    {icon && <LazyLoadSVG className="size-5" src={icon} />}

                    <div className="flex flex-col items-start">
                        <span>{workflowTrigger?.label || title}</span>

                        <span className="text-xs text-muted-foreground">
                            ({workflowTrigger?.name || workflowTrigger?.type})
                        </span>
                    </div>
                </div>
            </div>

            <div className="flex items-center">
                <span className="p-1 text-xs">{duration ?? 0}ms</span>
            </div>
        </li>
    );
};

export default WorkflowTriggerExecutionItem;
