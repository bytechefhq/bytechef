import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {EditIcon, EllipsisVerticalIcon, RefreshCcwIcon, Trash2Icon} from 'lucide-react';

interface ConnectedUserSheetDeleteDropdownMenuProps {
    changeConfigurationDisabled?: boolean;
    changeConfigurationDisabledReason?: string;
    onChangeConfigurationClick?: () => void;
    onDeleteClick: () => void;
    onEditClick?: () => void;
}

const ConnectedUserSheetDeleteDropdownMenu = ({
    changeConfigurationDisabled,
    changeConfigurationDisabledReason,
    onChangeConfigurationClick,
    onDeleteClick,
    onEditClick,
}: ConnectedUserSheetDeleteDropdownMenuProps) => (
    <DropdownMenu>
        <DropdownMenuTrigger asChild>
            <Button
                aria-label="More actions"
                icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                size="icon"
                variant="ghost"
            />
        </DropdownMenuTrigger>

        <DropdownMenuContent align="end">
            {onEditClick && <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />}

            {onChangeConfigurationClick && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span className="block">
                            <DropdownMenuItem
                                aria-label="Change configuration"
                                disabled={changeConfigurationDisabled || !!changeConfigurationDisabledReason}
                                icon={<RefreshCcwIcon />}
                                label="Change configuration…"
                                onClick={onChangeConfigurationClick}
                            />
                        </span>
                    </TooltipTrigger>

                    {changeConfigurationDisabledReason && (
                        <TooltipContent side="left">{changeConfigurationDisabledReason}</TooltipContent>
                    )}
                </Tooltip>
            )}

            <DropdownMenuItem icon={<Trash2Icon />} label="Delete" onClick={onDeleteClick} variant="destructive" />
        </DropdownMenuContent>
    </DropdownMenu>
);

export default ConnectedUserSheetDeleteDropdownMenu;
