import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';

import '@/shared/styles/dropdownMenu.css';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {ReactNode, useState} from 'react';

interface WorkflowListItemDropdownMenuProps {
    children?: ReactNode;
    dialogs?: ReactNode;
    onDelete: () => void;
    onEditClick: () => void;
    workflowLabel?: string | null;
}

const WorkflowListItemDropdownMenu = ({
    children,
    dialogs,
    onDelete,
    onEditClick,
    workflowLabel,
}: WorkflowListItemDropdownMenuProps) => {
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] = useState(false);

    const ariaLabel = workflowLabel ? `Workflow actions for ${workflowLabel}` : 'Workflow actions';

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label={ariaLabel}
                        className="-mr-px w-6 px-0"
                        icon={<EllipsisVerticalIcon />}
                        onClick={(event) => event.stopPropagation()}
                        size="icon"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end" className="p-0">
                    <DropdownMenuItem className="dropdown-menu-item" onClick={onEditClick}>
                        <EditIcon /> Edit
                    </DropdownMenuItem>

                    {children}

                    <DropdownMenuSeparator className="m-0" />

                    <DropdownMenuItem
                        className="dropdown-menu-item-destructive"
                        onClick={() => setShowDeleteWorkflowAlertDialog(true)}
                        variant="destructive"
                    >
                        <Trash2Icon /> Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            {showDeleteWorkflowAlertDialog && (
                <DeleteWorkflowAlertDialog
                    onClose={() => setShowDeleteWorkflowAlertDialog(false)}
                    onDelete={() => {
                        onDelete();

                        setShowDeleteWorkflowAlertDialog(false);
                    }}
                />
            )}

            {dialogs}
        </>
    );
};

export default WorkflowListItemDropdownMenu;
