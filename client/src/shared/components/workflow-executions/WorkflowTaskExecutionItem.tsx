import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import WorkflowExecutionBadge from '@/shared/components/workflow-executions/WorkflowExecutionBadge';
import {TaskExecution} from '@/shared/middleware/platform/workflow/execution';
import {twMerge} from 'tailwind-merge';

const WorkflowTaskExecutionItem = ({
    onClick,
    selected,
    taskExecution,
}: {
    selected?: boolean;
    taskExecution: TaskExecution;
    onClick?: () => void;
}) => {
    const {endDate, icon, startDate, title, workflowTask} = taskExecution;

    const duration = startDate && endDate && Math.round(endDate?.getTime() - startDate.getTime());

    return (
        <li
            className={twMerge(
                'flex w-full cursor-pointer items-center justify-between rounded-lg px-2 py-2 hover:bg-muted',
                selected && 'bg-muted/50 font-semibold'
            )}
            onClick={() => onClick && onClick()}
        >
            <div className="flex items-center gap-x-2 text-sm">
                <WorkflowExecutionBadge status={taskExecution.status} />

                <div className="flex items-center gap-x-1">
                    {icon && <LazyLoadSVG className="size-4" src={icon} />}

                    <div className="flex flex-col items-start">
                        <span>{title}</span>

                        <span className="text-xs text-muted-foreground">
                            ({workflowTask?.name || workflowTask?.type})
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

export default WorkflowTaskExecutionItem;
