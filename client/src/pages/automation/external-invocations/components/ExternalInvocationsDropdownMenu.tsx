import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {useStopJobMutation} from '@/shared/mutations/platform/jobs.mutations';
import {CircleStopIcon, EllipsisVerticalIcon, ViewIcon} from 'lucide-react';
import {toast} from 'sonner';

const RUNNING_STATUSES = new Set<string>(['APPROVAL_REQUIRED', 'SUSPENDED', 'TIMEOUT']);

interface ExternalInvocationsDropdownMenuProps {
    jobId?: number | string | null;
    onViewJob: (jobId: number) => void;
    status: string;
}

const ExternalInvocationsDropdownMenu = ({jobId, onViewJob, status}: ExternalInvocationsDropdownMenuProps) => {
    const stopJobMutation = useStopJobMutation({
        onSuccess: () => toast('Stopping Workflow Execution'),
    });

    const hasJob = jobId != null;

    const handleViewClick = () => {
        if (hasJob) {
            onViewJob(Number(jobId));
        }
    };

    const handleStopClick = () => {
        if (hasJob) {
            stopJobMutation.mutate(Number(jobId));
        }
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label="External invocation actions"
                    icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                    size="icon"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem disabled={!hasJob} icon={<ViewIcon />} label="View" onClick={handleViewClick} />

                <DropdownMenuItem
                    disabled={!hasJob || !RUNNING_STATUSES.has(status)}
                    icon={<CircleStopIcon />}
                    label="Stop"
                    onClick={handleStopClick}
                    variant="destructive"
                />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ExternalInvocationsDropdownMenu;
