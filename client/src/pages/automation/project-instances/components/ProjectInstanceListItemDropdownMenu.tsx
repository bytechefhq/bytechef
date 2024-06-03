import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {EllipsisVerticalIcon} from 'lucide-react';

interface ProjectInstanceListItemDropdownMenuProps {
    onDeleteClick: () => void;
    onEditClick: () => void;
    onEnableClick: () => void;
    onUpdateProjectVersionClick: () => void;
    projectInstanceEnabled: boolean;
}

const ProjectInstanceListItemDropdownMenu = ({
    onDeleteClick,
    onEditClick,
    onEnableClick,
    onUpdateProjectVersionClick,
    projectInstanceEnabled,
}: ProjectInstanceListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button size="icon" variant="ghost">
                    <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem disabled={projectInstanceEnabled} onClick={onEditClick}>
                    Edit
                </DropdownMenuItem>

                <DropdownMenuItem disabled={projectInstanceEnabled} onClick={onUpdateProjectVersionClick}>
                    Update Project Version
                </DropdownMenuItem>

                <DropdownMenuItem onClick={onEnableClick}>
                    {projectInstanceEnabled ? 'Disable' : 'Enable'}
                </DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem
                    className="text-destructive"
                    disabled={projectInstanceEnabled}
                    onClick={onDeleteClick}
                >
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectInstanceListItemDropdownMenu;
