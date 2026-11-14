import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {EditIcon, EllipsisVerticalIcon, KeyRoundIcon, RefreshCcwIcon, Trash2Icon} from 'lucide-react';

interface ConnectedUserSheetDeleteDropdownMenuProps {
    changeConfigurationDisabled?: boolean;
    changeConfigurationDisabledReason?: string;
    credentialsDisabledReason?: string;
    onChangeConfigurationClick?: () => void;
    onCredentialsClick?: () => void;
    onDeleteClick?: () => void;
    onEditClick?: () => void;
}

const ConnectedUserSheetDeleteDropdownMenu = ({
    changeConfigurationDisabled,
    changeConfigurationDisabledReason,
    credentialsDisabledReason,
    onChangeConfigurationClick,
    onCredentialsClick,
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

            {onCredentialsClick && (
                <DropdownMenuItem icon={<KeyRoundIcon />} label="Credentials" onClick={onCredentialsClick} />
            )}

            {credentialsDisabledReason && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span className="block">
                            <DropdownMenuItem disabled icon={<KeyRoundIcon />} label="Credentials" />
                        </span>
                    </TooltipTrigger>

                    <TooltipContent side="left">{credentialsDisabledReason}</TooltipContent>
                </Tooltip>
            )}

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

            {onDeleteClick && (
                <DropdownMenuItem icon={<Trash2Icon />} label="Delete" onClick={onDeleteClick} variant="destructive" />
            )}
        </DropdownMenuContent>
    </DropdownMenu>
);

export default ConnectedUserSheetDeleteDropdownMenu;
