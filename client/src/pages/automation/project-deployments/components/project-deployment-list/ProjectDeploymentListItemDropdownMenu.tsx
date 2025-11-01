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
    onUpdateProjectVersionClick: () => void;
}

const ProjectDeploymentListItemDropdownMenu = ({
    onDeleteClick,
    onEditClick,
    onUpdateProjectVersionClick,
}: ProjectDeploymentListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost" >
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem className="dropdown-menu-item" onClick={onEditClick}>Edit</DropdownMenuItem>

                <DropdownMenuItem className="dropdown-menu-item" onClick={onUpdateProjectVersionClick}>Update Project Version</DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-destructive dropdown-menu-item" onClick={onDeleteClick}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentListItemDropdownMenu;
