import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {ReactNode, useState} from 'react';

interface WorkflowListItemDropdownMenuProps {
    children?: ReactNode;
    dialogs?: ReactNode;
    onDelete: () => void;
    onEditClick: () => void;
    showDeleteAction?: boolean;
    showEditAction?: boolean;
    workflowLabel?: string | null;
}

const WorkflowListItemDropdownMenu = ({
    children,
    dialogs,
    onDelete,
    onEditClick,
    showDeleteAction = true,
    showEditAction = true,
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

                <DropdownMenuContent align="end">
                    {showEditAction && <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />}

                    {children}

                    {showDeleteAction && (
                        <>
                            <DropdownMenuSeparator />

                            <DropdownMenuItem
                                icon={<Trash2Icon />}
                                label="Delete"
                                onClick={() => setShowDeleteWorkflowAlertDialog(true)}
                                variant="destructive"
                            />
                        </>
                    )}
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
