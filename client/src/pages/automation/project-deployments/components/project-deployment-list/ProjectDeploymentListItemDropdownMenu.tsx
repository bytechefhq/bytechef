import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import EEVersion from '@/shared/edition/EEVersion';
import {EditIcon, EllipsisVerticalIcon, RefreshCcwIcon, RocketIcon, Trash2Icon} from 'lucide-react';

interface ProjectDeploymentListItemDropdownMenuProps {
    onChangeProjectVersionClick: () => void;
    onDeleteClick: () => void;
    onEditClick: () => void;
    onPromoteClick?: () => void;
    showPromoteToEnvironment?: boolean;
}

const ProjectDeploymentListItemDropdownMenu = ({
    onChangeProjectVersionClick,
    onDeleteClick,
    onEditClick,
    onPromoteClick,
    showPromoteToEnvironment = false,
}: ProjectDeploymentListItemDropdownMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />

                <DropdownMenuItem
                    icon={<RefreshCcwIcon />}
                    label="Change Project Version"
                    onClick={onChangeProjectVersionClick}
                />

                {showPromoteToEnvironment && onPromoteClick && (
                    <EEVersion hidden={true}>
                        <DropdownMenuItem
                            icon={<RocketIcon />}
                            label="Promote to environment…"
                            onClick={onPromoteClick}
                        />
                    </EEVersion>
                )}

                <DropdownMenuSeparator />

                <DropdownMenuItem icon={<Trash2Icon />} label="Delete" onClick={onDeleteClick} variant="destructive" />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectDeploymentListItemDropdownMenu;
