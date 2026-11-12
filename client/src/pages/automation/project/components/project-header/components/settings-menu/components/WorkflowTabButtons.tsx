import {DropdownMenuItem, DropdownMenuSeparator} from '@/components/DropdownMenu/DropdownMenu';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {CopyIcon, DownloadIcon, EditIcon, Share2Icon, Trash2Icon} from 'lucide-react';

const WorkflowTabButtons = ({
    onDuplicateWorkflow,
    onShareWorkflow,
    onShowDeleteWorkflowAlertDialog,
    onShowEditWorkflowDialog,
    workflowId,
}: {
    onDuplicateWorkflow: () => void;
    onShareWorkflow: () => void;
    onShowEditWorkflowDialog: () => void;
    onShowDeleteWorkflowAlertDialog: () => void;
    workflowId: string;
}) => {
    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.workflows);

    return (
        <>
            <DropdownMenuItem icon={<EditIcon />} label="Edit" onSelect={onShowEditWorkflowDialog} />

            <DropdownMenuItem icon={<CopyIcon />} label="Duplicate" onSelect={onDuplicateWorkflow} />

            <DropdownMenuItem icon={<Share2Icon />} label="Share" onSelect={onShareWorkflow} />

            {templatesSubmissionForm && (
                <DropdownMenuItem
                    icon={<Share2Icon />}
                    label="Share with Community"
                    onSelect={() => window.open(templatesSubmissionForm, '_blank')}
                />
            )}

            <DropdownMenuItem
                icon={<DownloadIcon />}
                label="Export"
                onSelect={() => (window.location.href = `/api/automation/internal/workflows/${workflowId}/export`)}
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
};

export default WorkflowTabButtons;
