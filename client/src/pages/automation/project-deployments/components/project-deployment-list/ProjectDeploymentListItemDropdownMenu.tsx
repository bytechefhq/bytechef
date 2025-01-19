import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {EllipsisVerticalIcon} from 'lucide-react';

interface ProjectDeploymentListItemDropdownMenuProps {
    onDeleteClick: () => void;
    onEditClick: () => void;
    onEnableClick: () => void;
    onUpdateProjectVersionClick: () => void;
    projectDeploymentEnabled: boolean;
}

const ProjectDeploymentListItemDropdownMenu = ({
    onDeleteClick,
    onEditClick,
    onEnableClick,
    onUpdateProjectVersionClick,
    projectDeploymentEnabled,
}: ProjectDeploymentListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={onEditClick}>Edit</DropdownMenuItem>

                <DropdownMenuItem onClick={onUpdateProjectVersionClick}>Update Project Version</DropdownMenuItem>

                <DropdownMenuItem onClick={onEnableClick}>
                    {projectDeploymentEnabled ? 'Disable' : 'Enable'}
                </DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-destructive" onClick={onDeleteClick}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentListItemDropdownMenu;
