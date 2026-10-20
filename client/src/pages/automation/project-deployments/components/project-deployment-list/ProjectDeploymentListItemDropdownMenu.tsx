import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
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
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />

                {onOpenProjectClick && (
                    <DropdownMenuItem
                        icon={<SquareArrowOutUpRightIcon />}
                        label="Open Project"
                        onClick={onOpenProjectClick}
                    />
                )}

                <DropdownMenuItem
                    icon={<RefreshCcwIcon />}
                    label="Change Project Version"
                    onClick={onChangeProjectVersionClick}
                />

                <DropdownMenuSeparator />

                <DropdownMenuItem icon={<Trash2Icon />} label="Delete" onClick={onDeleteClick} variant="destructive" />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentListItemDropdownMenu;
