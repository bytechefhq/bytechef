import {DropdownMenuItem, DropdownMenuSeparator} from '@/components/DropdownMenu/DropdownMenu';
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
}) => (
    <>
        <DropdownMenuItem icon={<EditIcon />} label="Edit" onSelect={onShowEditIntegrationDialogClick} />

        {!codeWorkflow && (
            <>
                <DropdownMenuSeparator />

                <DropdownMenuItem icon={<PlusIcon />} label="New Workflow" onSelect={onNewWorkflowClick} />

                <DropdownMenuItem icon={<UploadIcon />} label="Import Workflow" onSelect={onImportWorkflowClick} />
            </>
        )}

        <DropdownMenuSeparator />

        <DropdownMenuItem
            icon={<HistoryIcon />}
            label="Integration History"
            onSelect={onShowIntegrationVersionHistorySheet}
        />

        <DropdownMenuSeparator />

        <DropdownMenuItem
            icon={<Trash2Icon />}
            label="Delete"
            onSelect={onDeleteIntegrationClick}
            variant="destructive"
        />
    </>
);

export default IntegrationTabButtons;
