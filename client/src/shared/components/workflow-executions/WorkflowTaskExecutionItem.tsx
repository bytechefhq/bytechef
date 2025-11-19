import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import {TaskExecution} from '@/shared/middleware/platform/workflow/execution';
import {AlertTriangleIcon, CheckIcon} from 'lucide-react';

const WorkflowTaskExecutionItem = ({taskExecution}: {taskExecution: TaskExecution}) => {
    const {endDate, icon, startDate, title, workflowTask} = taskExecution;

    const duration = startDate && endDate && Math.round(endDate?.getTime() - startDate.getTime());

    return (
        <li className="flex w-full cursor-pointer items-center justify-between rounded-lg p-0 hover:bg-inherit">
            <div className="flex items-center gap-x-2 text-sm">
                {taskExecution.status === 'COMPLETED' ? (
                    <CheckIcon className="size-4 text-success" />
                ) : (
                    <AlertTriangleIcon className="size-4 text-destructive" />
                )}

                <div className="flex items-center gap-x-1">
                    {icon && <LazyLoadSVG className="size-5" src={icon} />}

                    <div className="flex flex-col items-start">
                        <span>{workflowTask?.label || title}</span>

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
