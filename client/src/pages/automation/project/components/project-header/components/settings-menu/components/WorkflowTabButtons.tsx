import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {Separator} from '@/components/ui/separator';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {CopyIcon, DownloadIcon, EditIcon, Share2Icon, Trash2Icon} from 'lucide-react';
import {MouseEvent} from 'react';

const WorkflowTabButtons = ({
    onCloseDropdownMenu,
    onDuplicateWorkflow,
    onShareWorkflow,
    onShowDeleteWorkflowAlertDialog,
    onShowEditWorkflowDialog,
    workflowId,
}: {
    onCloseDropdownMenu: () => void;
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

    const handleButtonClick = (event: MouseEvent<HTMLDivElement>) => {
        if ((event.target as HTMLElement).tagName === 'BUTTON') {
            onCloseDropdownMenu();
        }
    };

    return (
        <div className="flex flex-col" onClick={handleButtonClick}>
            {canEditWorkflow && (
                <Button
                    className="dropdown-menu-item"
                    icon={<EditIcon />}
                    label="Edit"
                    onClick={onShowEditWorkflowDialog}
                    variant="ghost"
                />
            )}

            {canDuplicateWorkflow && (
                <Button
                    className="dropdown-menu-item"
                    icon={<CopyIcon />}
                    label="Duplicate"
                    onClick={onDuplicateWorkflow}
                    variant="ghost"
                />
            )}

            {canEditWorkflow && (
                <Button
                    className="dropdown-menu-item"
                    icon={<Share2Icon />}
                    label="Share"
                    onClick={onShareWorkflow}
                    variant="ghost"
                />
            )}

            {templatesSubmissionForm && (
                <Button
                    className="dropdown-menu-item"
                    icon={<Share2Icon />}
                    label="Share with Community"
                    onClick={() => window.open(templatesSubmissionForm, '_blank')}
                    variant="ghost"
                />
            )}

            <Button
                className="dropdown-menu-item"
                icon={<DownloadIcon />}
                label="Export"
                onClick={() => (window.location.href = `/api/automation/internal/workflows/${workflowId}/export`)}
                variant="ghost"
            />

            {canDeleteWorkflow && (
                <>
                    <Separator />

                    <Button
                        className="dropdown-menu-item-destructive"
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={() => onShowDeleteWorkflowAlertDialog()}
                        variant="ghost"
                    />
                </>
            )}
        </div>
    );
};

export default WorkflowTabButtons;
