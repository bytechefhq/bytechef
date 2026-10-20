import {DropdownMenuItem, DropdownMenuSeparator} from '@/components/DropdownMenu/DropdownMenu';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {UNION_ENVIRONMENT_ID, useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {EditIcon, HistoryIcon, PlusIcon, Trash2Icon, UploadIcon} from 'lucide-react';

const IntegrationTabButtons = ({
    codeWorkflow = false,
    onDeleteIntegrationClick,
    onImportWorkflowClick,
    onNewWorkflowClick,
    onShowEditIntegrationDialogClick,
    onShowIntegrationVersionHistorySheet,
}: {
    codeWorkflow?: boolean;
    onDeleteIntegrationClick: () => void;
    onImportWorkflowClick: () => void;
    onNewWorkflowClick: () => void;
    onShowEditIntegrationDialogClick: () => void;
    onShowIntegrationVersionHistorySheet: () => void;
}) => {
    const canCreateWorkflow = useHasEmbeddedScope('WORKFLOW_CREATE', DEVELOPMENT_ENVIRONMENT);
    const canDeleteIntegration = useHasEmbeddedScope('INTEGRATION_DELETE', UNION_ENVIRONMENT_ID);
    const canEditIntegration = useHasEmbeddedScope('WORKFLOW_EDIT', UNION_ENVIRONMENT_ID);

    return (
        <>
            {canEditIntegration && (
                <>
                    <DropdownMenuItem icon={<EditIcon />} label="Edit" onSelect={onShowEditIntegrationDialogClick} />

                    <DropdownMenuSeparator />
                </>
            )}

            {!codeWorkflow && canCreateWorkflow && (
                <>
                    <DropdownMenuItem icon={<PlusIcon />} label="New Workflow" onSelect={onNewWorkflowClick} />

                    <DropdownMenuItem icon={<UploadIcon />} label="Import Workflow" onSelect={onImportWorkflowClick} />

                    <DropdownMenuSeparator />
                </>
            )}

            <DropdownMenuItem
                icon={<HistoryIcon />}
                label="Integration History"
                onSelect={onShowIntegrationVersionHistorySheet}
            />

            {canDeleteIntegration && (
                <>
                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onSelect={onDeleteIntegrationClick}
                        variant="destructive"
                    />
                </>
            )}
        </>
    );
};

export default IntegrationTabButtons;
