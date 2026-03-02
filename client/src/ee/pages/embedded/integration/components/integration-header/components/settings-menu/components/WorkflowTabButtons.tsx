import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {Separator} from '@/components/ui/separator';
import {DownloadIcon, EditIcon, Trash2Icon} from 'lucide-react';
import {MouseEvent} from 'react';

const WorkflowTabButtons = ({
    onCloseDropdownMenu,
    onShowDeleteWorkflowAlertDialog,
    onShowEditWorkflowDialog,
    workflowId,
}: {
    onCloseDropdownMenu: () => void;
    onShowDeleteWorkflowAlertDialog: () => void;
    onShowEditWorkflowDialog: () => void;
    workflowId: string;
}) => {
    const handleButtonClick = (event: MouseEvent<HTMLDivElement>) => {
        if ((event.target as HTMLElement).tagName === 'BUTTON') {
            onCloseDropdownMenu();
        }
    };

    return (
        <div className="flex flex-col" onClick={handleButtonClick}>
            <Button
                className="dropdown-menu-item"
                icon={<EditIcon />}
                label="Edit"
                onClick={onShowEditWorkflowDialog}
                variant="ghost"
            />

            <Button
                className="dropdown-menu-item"
                icon={<DownloadIcon />}
                label="Export"
                onClick={() => (window.location.href = `/api/embedded/internal/workflows/${workflowId}/export`)}
                variant="ghost"
            />

            <Separator />

            <Button
                className="dropdown-menu-item-destructive"
                icon={<Trash2Icon />}
                label="Delete"
                onClick={() => onShowDeleteWorkflowAlertDialog()}
                variant="ghost"
            />
        </div>
    );
};

export default WorkflowTabButtons;
