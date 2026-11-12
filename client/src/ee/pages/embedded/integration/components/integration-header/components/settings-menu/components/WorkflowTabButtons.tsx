import {DropdownMenuItem, DropdownMenuSeparator} from '@/components/DropdownMenu/DropdownMenu';
import {DownloadIcon, EditIcon, Trash2Icon} from 'lucide-react';

const WorkflowTabButtons = ({
    onShowDeleteWorkflowAlertDialog,
    onShowEditWorkflowDialog,
    workflowId,
}: {
    onShowDeleteWorkflowAlertDialog: () => void;
    onShowEditWorkflowDialog: () => void;
    workflowId: string;
}) => (
    <>
        <DropdownMenuItem icon={<EditIcon />} label="Edit" onSelect={onShowEditWorkflowDialog} />

        <DropdownMenuItem
            icon={<DownloadIcon />}
            label="Export"
            onSelect={() => (window.location.href = `/api/embedded/internal/workflows/${workflowId}/export`)}
        />

        <DropdownMenuSeparator />

        <DropdownMenuItem
            icon={<Trash2Icon />}
            label="Delete"
            onSelect={onShowDeleteWorkflowAlertDialog}
            variant="destructive"
        />
    </>
);

export default WorkflowTabButtons;
