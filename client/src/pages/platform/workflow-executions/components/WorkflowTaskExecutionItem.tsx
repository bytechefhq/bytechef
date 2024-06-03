import WorkflowExecutionBadge from '@/pages/platform/workflow-executions/components/WorkflowExecutionBadge';
import {TaskExecutionModel} from '@/shared/middleware/platform/workflow/execution';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const WorkflowTaskExecutionItem = ({
    onClick,
    selected,
    taskExecution,
}: {
    selected?: boolean;
    taskExecution: TaskExecutionModel;
    onClick?: () => void;
}) => {
    const {component, endDate, startDate, workflowTask} = taskExecution;

    const duration = startDate && endDate && Math.round(endDate?.getTime() - startDate.getTime());

    return (
        <li
            className={twMerge(
                'flex w-full cursor-pointer items-center justify-between p-4 hover:bg-muted',
                selected && 'font-semibold bg-gray-50'
            )}
            onClick={() => onClick && onClick()}
        >
            <div className="flex items-center gap-x-2 text-sm">
                <WorkflowExecutionBadge status={taskExecution.status} />

                <div className="flex items-center gap-x-1">
                    {taskExecution?.component?.icon && (
                        <InlineSVG className="size-4" src={taskExecution?.component?.icon} />
                    )}

                    <span>{component?.title}</span>

                    <span className="text-xs text-muted-foreground">({workflowTask?.name || workflowTask?.type})</span>
                </div>
            </div>

            <div className="flex items-center">
                <span className="text-xs">{duration ?? 0}ms</span>
            </div>
        </li>
    );
};

export default WorkflowTaskExecutionItem;
