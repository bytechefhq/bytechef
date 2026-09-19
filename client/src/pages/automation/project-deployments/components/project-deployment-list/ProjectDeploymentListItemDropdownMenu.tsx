import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {EditIcon, EllipsisVerticalIcon, RefreshCcwIcon, Trash2Icon} from 'lucide-react';

interface ProjectDeploymentListItemDropdownMenuProps {
    onChangeProjectVersionClick: () => void;
    onDeleteClick: () => void;
    onEditClick: () => void;
}

const ProjectDeploymentListItemDropdownMenu = ({
    onChangeProjectVersionClick,
    onDeleteClick,
    onEditClick,
}: ProjectDeploymentListItemDropdownMenuProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    // Delete is ADMIN-tier on the server (ProjectDeploymentFacadeImpl.deleteProjectDeployment), so offering it to a member without DEPLOYMENT_DELETE means a confirm dialog
    // that ends in a 403.
    const canDelete = useHasWorkspaceScope(currentWorkspaceId, 'DEPLOYMENT_DELETE');
    const canUpdate = useHasWorkspaceScope(currentWorkspaceId, 'DEPLOYMENT_CREATE');

    if (!canUpdate && !canDelete) {
        return null;
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label="More Deployment Actions"
                    icon={<EllipsisVerticalIcon />}
                    size="icon"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="p-0">
                {canUpdate && (
                    <>
                        <DropdownMenuItem className="dropdown-menu-item" onClick={onEditClick}>
                            <EditIcon /> Edit
                        </DropdownMenuItem>

                        <DropdownMenuItem className="dropdown-menu-item" onClick={onChangeProjectVersionClick}>
                            <RefreshCcwIcon /> Change Project Version
                        </DropdownMenuItem>
                    </>
                )}

                {canDelete && (
                    <>
                        {canUpdate && <DropdownMenuSeparator className="m-0" />}

                        <DropdownMenuItem
                            className="dropdown-menu-item-destructive"
                            onClick={onDeleteClick}
                            variant="destructive"
                        >
                            <Trash2Icon /> Delete
                        </DropdownMenuItem>
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentListItemDropdownMenu;
