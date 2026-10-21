import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {WorkflowExecutionKeys as EmbeddedWorkflowExecutionKeys} from '@/ee/shared/queries/embedded/workflowExecutions.queries';
import {useStopJobMutation} from '@/shared/mutations/platform/jobs.mutations';
import {WorkflowExecutionKeys as AutomationWorkflowExecutionKeys} from '@/shared/queries/automation/workflowExecutions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {CircleStopIcon, EllipsisVerticalIcon, ViewIcon} from 'lucide-react';
import {toast} from 'sonner';

interface EmbeddedWorkflowExecutionsDropdownMenuProps {
    jobId?: string;
    jobStatus?: string;
    onView: () => void;
}

const EmbeddedWorkflowExecutionsDropdownMenu = ({
    jobId,
    jobStatus,
    onView,
}: EmbeddedWorkflowExecutionsDropdownMenuProps) => {
    const queryClient = useQueryClient();

    const stopJobMutation = useStopJobMutation({
        onSuccess: () => {
            toast('Stopping Workflow Execution');

            void queryClient.invalidateQueries({
                queryKey: EmbeddedWorkflowExecutionKeys.workflowExecutions,
            });

            void queryClient.invalidateQueries({
                queryKey: AutomationWorkflowExecutionKeys.workflowExecutions,
            });
        },
    });

    const stopDisabled = jobStatus !== 'STARTED';

    const handleStopWorkflowExecutionClick = () => {
        if (jobId != null) {
            stopJobMutation.mutate(Number(jobId));
        }
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                    onClick={(event) => event.stopPropagation()}
                    size="icon"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" onClick={(event) => event.stopPropagation()}>
                <DropdownMenuItem icon={<ViewIcon />} label="View" onClick={onView} />

                <DropdownMenuItem
                    disabled={stopDisabled}
                    icon={<CircleStopIcon />}
                    label="Stop"
                    onClick={handleStopWorkflowExecutionClick}
                    variant="destructive"
                />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default EmbeddedWorkflowExecutionsDropdownMenu;
