import {DropdownMenuItem, DropdownMenuSeparator} from '@/components/DropdownMenu/DropdownMenu';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {DownloadIcon, EditIcon, Trash2Icon} from 'lucide-react';

const WorkflowTabButtons = ({
    onShowDeleteWorkflowAlertDialog,
    onShowEditWorkflowDialog,
    workflowId,
}: {
    onShowDeleteWorkflowAlertDialog: () => void;
    onShowEditWorkflowDialog: () => void;
    workflowId: string;
}) => {
    const canDeleteWorkflow = useHasEmbeddedScope('WORKFLOW_DELETE', DEVELOPMENT_ENVIRONMENT);
    const canEditWorkflow = useHasEmbeddedScope('WORKFLOW_EDIT', DEVELOPMENT_ENVIRONMENT);

    return (
        <>
            {canEditWorkflow && (
                <DropdownMenuItem icon={<EditIcon />} label="Edit" onSelect={onShowEditWorkflowDialog} />
            )}

            <DropdownMenuItem
                icon={<DownloadIcon />}
                label="Export"
                onSelect={() => (window.location.href = `/api/embedded/internal/workflows/${workflowId}/export`)}
            />

            {canDeleteWorkflow && (
                <>
                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onSelect={onShowDeleteWorkflowAlertDialog}
                        variant="destructive"
                    />
                </>
            )}
        </>
    );
};

export default WorkflowTabButtons;
