import {DropdownMenuItem, DropdownMenuSeparator} from '@/components/DropdownMenu/DropdownMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
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
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const templatesSubmissionForm = useApplicationInfoStore((state) => state.templatesSubmissionForm.workflows);

    const canCreateWorkflow = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_CREATE', DEVELOPMENT_ENVIRONMENT);
    const canDeleteWorkflow = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_DELETE', DEVELOPMENT_ENVIRONMENT);
    const canEditWorkflow = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_EDIT', DEVELOPMENT_ENVIRONMENT);
    const canViewWorkflow = useHasWorkspaceScope(currentWorkspaceId, 'WORKFLOW_VIEW', DEVELOPMENT_ENVIRONMENT);

    const canDuplicateWorkflow = canCreateWorkflow && canViewWorkflow;

    return (
        <>
            {canEditWorkflow && (
                <DropdownMenuItem icon={<EditIcon />} label="Edit" onSelect={onShowEditWorkflowDialog} />
            )}

            {canDuplicateWorkflow && (
                <DropdownMenuItem icon={<CopyIcon />} label="Duplicate" onSelect={onDuplicateWorkflow} />
            )}

            {canEditWorkflow && <DropdownMenuItem icon={<Share2Icon />} label="Share" onSelect={onShareWorkflow} />}

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
