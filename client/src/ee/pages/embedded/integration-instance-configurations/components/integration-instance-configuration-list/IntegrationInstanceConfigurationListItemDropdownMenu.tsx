import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {EditIcon, EllipsisVerticalIcon, RefreshCcwIcon, Trash2Icon, UsersIcon} from 'lucide-react';

interface IntegrationInstanceConfigurationListItemDropdownMenuProps {
    canDelete: boolean;
    canEdit: boolean;
    canMoveUsers: boolean;
    onDeleteClick: () => void;
    onEditClick: () => void;
    moveUsersDisabled?: boolean;
    moveUsersDisabledReason?: string;
    onMoveUsersClick: () => void;
    onUpdateIntegrationVersionClick: () => void;
}

const IntegrationInstanceConfigurationListItemDropdownMenu = ({
    canDelete,
    canEdit,
    canMoveUsers,
    moveUsersDisabled,
    moveUsersDisabledReason,
    onDeleteClick,
    onEditClick,
    onMoveUsersClick,
    onUpdateIntegrationVersionClick,
}: IntegrationInstanceConfigurationListItemDropdownMenuProps) => {
    if (!canEdit && !canDelete && !canMoveUsers) {
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

                {canMoveUsers && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="block">
                                <DropdownMenuItem
                                    aria-label="Move Users"
                                    disabled={moveUsersDisabled || !!moveUsersDisabledReason}
                                    icon={<UsersIcon />}
                                    label="Move Users…"
                                    onClick={onMoveUsersClick}
                                />
                            </span>
                        </TooltipTrigger>

                        {moveUsersDisabledReason && (
                            <TooltipContent side="left">{moveUsersDisabledReason}</TooltipContent>
                        )}
                    </Tooltip>
                )}

                {(canEdit || canMoveUsers) && canDelete && <DropdownMenuSeparator />}

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
