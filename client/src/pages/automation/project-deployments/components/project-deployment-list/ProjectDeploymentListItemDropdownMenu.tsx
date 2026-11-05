import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {EditIcon, EllipsisVerticalIcon, RefreshCcwIcon, SquareArrowOutUpRightIcon, Trash2Icon} from 'lucide-react';

interface ProjectDeploymentListItemDropdownMenuProps {
    onChangeProjectVersionClick: () => void;
    onDeleteClick: () => void;
    onEditClick: () => void;
    onOpenProjectClick?: () => void;
}

const ProjectDeploymentListItemDropdownMenu = ({
    onChangeProjectVersionClick,
    onDeleteClick,
    onEditClick,
    onOpenProjectClick,
}: ProjectDeploymentListItemDropdownMenuProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    // Delete is ADMIN-tier on the server (ProjectDeploymentFacadeImpl.deleteProjectDeployment), so offering it to a member without DEPLOYMENT_DELETE means a confirm dialog
    // that ends in a 403.
    const canDelete = useHasWorkspaceScope(currentWorkspaceId, 'DEPLOYMENT_DELETE');
    const canUpdate = useHasWorkspaceScope(currentWorkspaceId, 'DEPLOYMENT_CREATE');

    if (!canUpdate && !canDelete && !onOpenProjectClick) {
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

            <DropdownMenuContent align="end">
                {canUpdate && <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />}

                {onOpenProjectClick && (
                    <DropdownMenuItem
                        icon={<SquareArrowOutUpRightIcon />}
                        label="Open Project"
                        onClick={onOpenProjectClick}
                    />
                )}

                {canUpdate && (
                    <DropdownMenuItem
                        icon={<RefreshCcwIcon />}
                        label="Change Project Version"
                        onClick={onChangeProjectVersionClick}
                    />
                )}

                {canDelete && (
                    <>
                        {(canUpdate || onOpenProjectClick) && <DropdownMenuSeparator />}

                        <DropdownMenuItem
                            icon={<Trash2Icon />}
                            label="Delete"
                            onClick={onDeleteClick}
                            variant="destructive"
                        />
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentListItemDropdownMenu;
