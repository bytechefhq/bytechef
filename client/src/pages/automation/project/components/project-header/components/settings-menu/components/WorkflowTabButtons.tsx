import {Button} from '@/components/ui/button';
import {Separator} from '@/components/ui/separator';
import {CopyIcon, EditIcon, Trash2Icon, UploadIcon} from 'lucide-react';

const WorkflowTabButtons = ({
    onCloseDropdownMenu,
    onDuplicateWorkflow,
    onShowDeleteWorkflowAlertDialog,
    onShowEditWorkflowDialog,
    workflowId,
}: {
    onCloseDropdownMenu: () => void;
    onDuplicateWorkflow: () => void;
    onShowEditWorkflowDialog: () => void;
    onShowDeleteWorkflowAlertDialog: () => void;
    workflowId: string;
}) => {
    const handleButtonClick = (event: React.MouseEvent<HTMLDivElement>) => {
        if ((event.target as HTMLElement).tagName === 'BUTTON') {
            onCloseDropdownMenu();
        }
    };

    return (
        <div className="flex flex-col" onClick={handleButtonClick}>
            <Button
                className="justify-start rounded-none hover:bg-surface-neutral-primary-hover"
                onClick={onShowEditWorkflowDialog}
                variant="ghost"
            >
                <EditIcon /> Edit
            </Button>

            <Button
                className="justify-start rounded-none hover:bg-surface-neutral-primary-hover"
                onClick={onDuplicateWorkflow}
                variant="ghost"
            >
                <CopyIcon /> Duplicate
            </Button>

            <Button
                className="justify-start rounded-none hover:bg-surface-neutral-primary-hover"
                onClick={() => (window.location.href = `/api/automation/internal/workflows/${workflowId}/export`)}
                variant="ghost"
            >
                <UploadIcon /> Export
            </Button>

            <Separator />

            <Button
                className="justify-start rounded-none text-destructive hover:bg-surface-error-secondary hover:text-destructive"
                onClick={() => onShowDeleteWorkflowAlertDialog()}
                variant="ghost"
            >
                <Trash2Icon /> Delete
            </Button>
        </div>
    );
};

export default WorkflowTabButtons;
