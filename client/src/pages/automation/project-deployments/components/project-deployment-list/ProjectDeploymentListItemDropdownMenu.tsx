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
    onChangeProjectVersionClick: () => void;
    onDeleteClick: () => void;
    onEditClick: () => void;
}

const ProjectDeploymentListItemDropdownMenu = ({
    onChangeProjectVersionClick,
    onDeleteClick,
    onEditClick,
}: ProjectDeploymentListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="p-0">
                <DropdownMenuItem className="dropdown-menu-item" onClick={onEditClick}>
                    <EditIcon /> Edit
                </DropdownMenuItem>

                <DropdownMenuItem className="dropdown-menu-item" onClick={onChangeProjectVersionClick}>
                    <RefreshCcwIcon /> Change Project Version
                </DropdownMenuItem>

                <DropdownMenuSeparator className="m-0" />

                <DropdownMenuItem className="dropdown-menu-item-destructive" onClick={onDeleteClick}>
                    <Trash2Icon /> Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentListItemDropdownMenu;
