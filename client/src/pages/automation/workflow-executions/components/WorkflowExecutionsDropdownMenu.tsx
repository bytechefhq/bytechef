import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {useRestartJobMutation, useStopJobMutation} from '@/shared/mutations/platform/jobs.mutations';
import {WorkflowExecutionKeys} from '@/shared/queries/automation/workflowExecutions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {CircleStopIcon, EllipsisVerticalIcon, RotateCcwIcon, ViewIcon} from 'lucide-react';
import {toast} from 'sonner';

import useWorkflowExecutionSheetStore from '../stores/useWorkflowExecutionSheetStore';

const RESTARTABLE_JOB_STATUSES = new Set<string>(['FAILED', 'STOPPED']);

const WorkflowExecutionsDropdownMenu = ({execution}: {execution: WorkflowExecution}) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const {setWorkflowExecutionId, setWorkflowExecutionSheetOpen} = useWorkflowExecutionSheetStore();

    const canControlWorkflowExecution = useHasWorkspaceScope(currentWorkspaceId, 'DEPLOYMENT_EDIT');
    const queryClient = useQueryClient();

    const restartJobMutation = useRestartJobMutation({
        onSuccess: () => {
            toast('Restarting Workflow Execution');

            void queryClient.invalidateQueries({
                queryKey: WorkflowExecutionKeys.workflowExecutions,
            });
        },
    });

    const stopJobMutation = useStopJobMutation({
        onSuccess: () => {
            toast('Stopping Workflow Execution');

            void queryClient.invalidateQueries({
                queryKey: WorkflowExecutionKeys.workflowExecutions,
            });
        },
    });

    const restartDisabled =
        !RESTARTABLE_JOB_STATUSES.has(execution.job?.status ?? '') || execution.job?.parentTaskExecutionId != null;
    const stopDisabled = execution.job?.status !== 'STARTED';

    const handleViewClick = () => {
        const id = execution.id;

        if (id != null) {
            setWorkflowExecutionId(id, execution.job ? 'JOB' : 'TRIGGER_EXECUTION');
            setWorkflowExecutionSheetOpen(true);
        }
    };

    const handleRestartWorkflowExecutionClick = () => {
        const jobId = execution.job?.id;

        if (jobId != null) {
            restartJobMutation.mutate(Number(jobId));
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

            <DropdownMenuContent align="end" onClick={(e) => e.stopPropagation()}>
                <DropdownMenuItem icon={<ViewIcon />} label="View" onClick={handleViewClick} />

                {canControlWorkflowExecution && (
                    <>
                        <DropdownMenuItem
                            disabled={restartDisabled}
                            icon={<RotateCcwIcon />}
                            label="Restart"
                            onClick={handleRestartWorkflowExecutionClick}
                        />

                        <DropdownMenuItem
                            disabled={stopDisabled}
                            icon={<CircleStopIcon />}
                            label="Stop"
                            onClick={handleStopWorkflowExecutionClick}
                            variant="destructive"
                        />
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default WorkflowExecutionsDropdownMenu;
