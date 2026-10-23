import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {EditIcon, EllipsisVerticalIcon, RefreshCcwIcon, Trash2Icon} from 'lucide-react';

interface IntegrationInstanceConfigurationListItemDropdownMenuProps {
    canDelete: boolean;
    canEdit: boolean;
    onDeleteClick: () => void;
    onEditClick: () => void;
    onUpdateIntegrationVersionClick: () => void;
}

const IntegrationInstanceConfigurationListItemDropdownMenu = ({
    canDelete,
    canEdit,
    onDeleteClick,
    onEditClick,
    onUpdateIntegrationVersionClick,
}: IntegrationInstanceConfigurationListItemDropdownMenuProps) => {
    if (!canEdit && !canDelete) {
        return null;
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                {canEdit && (
                    <>
                        <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />

                        <DropdownMenuItem
                            icon={<RefreshCcwIcon />}
                            label="Update Integration Version"
                            onClick={onUpdateIntegrationVersionClick}
                        />
                    </>
                )}

                {canEdit && canDelete && <DropdownMenuSeparator />}

                {canDelete && (
                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={onDeleteClick}
                        variant="destructive"
                    />
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default IntegrationInstanceConfigurationListItemDropdownMenu;
