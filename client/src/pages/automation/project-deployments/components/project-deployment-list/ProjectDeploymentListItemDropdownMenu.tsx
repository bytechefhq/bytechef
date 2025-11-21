import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {EditIcon, EllipsisVerticalIcon, RefreshCcwIcon, Trash2Icon} from 'lucide-react';

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
                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="p-0">
                <DropdownMenuItem className="dropdown-menu-item" onClick={onEditClick}>
                    <EditIcon /> Edit
                </DropdownMenuItem>

                <DropdownMenuItem className="dropdown-menu-item" onClick={onUpdateProjectVersionClick}>
                    <RefreshCcwIcon /> Update Project Version
                </DropdownMenuItem>

                <DropdownMenuSeparator className="m-0" />

                <DropdownMenuItem className="text-destructive dropdown-menu-item" onClick={onDeleteClick}>
                    Delete
                <DropdownMenuItem className="dropdown-menu-item-destructive" onClick={onDeleteClick}>
                    <Trash2Icon /> Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentListItemDropdownMenu;
