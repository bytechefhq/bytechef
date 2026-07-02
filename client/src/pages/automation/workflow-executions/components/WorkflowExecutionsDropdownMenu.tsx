import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {useStopJobMutation} from '@/shared/mutations/platform/jobs.mutations';
import {WorkflowExecutionKeys} from '@/shared/queries/automation/workflowExecutions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {CircleStopIcon, EllipsisVerticalIcon, ViewIcon} from 'lucide-react';
import {toast} from 'sonner';

import useWorkflowExecutionSheetStore from '../stores/useWorkflowExecutionSheetStore';

const WorkflowExecutionsDropdownMenu = ({execution}: {execution: WorkflowExecution}) => {
    const queryClient = useQueryClient();

    const {setWorkflowExecutionId, setWorkflowExecutionSheetOpen} = useWorkflowExecutionSheetStore();

    const stopJobMutation = useStopJobMutation({
        onSuccess: () => {
            toast('Stopping Workflow Execution');

            queryClient.invalidateQueries({
                queryKey: WorkflowExecutionKeys.workflowExecutions,
            });
        },
    });

    const disabled = execution.job?.status !== 'STARTED';

    const handleViewClick = () => {
        const id = execution.id;

        if (id != null) {
            setWorkflowExecutionId(id);
            setWorkflowExecutionSheetOpen(true);
        }
    };

    const handleStopWorkflowExecutionClick = () => {
        const jobId = execution.job?.id;

        if (jobId != null) {
            stopJobMutation.mutate(Number(jobId));
        }
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                    size="icon"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="center" onClick={(e) => e.stopPropagation()}>
                <DropdownMenuItem className="dropdown-menu-item" onClick={handleViewClick}>
                    <ViewIcon /> View
                </DropdownMenuItem>

                <DropdownMenuItem
                    className="dropdown-menu-item-destructive"
                    disabled={disabled}
                    onClick={handleStopWorkflowExecutionClick}
                    variant="destructive"
                >
                    <CircleStopIcon /> Stop
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default WorkflowExecutionsDropdownMenu;
