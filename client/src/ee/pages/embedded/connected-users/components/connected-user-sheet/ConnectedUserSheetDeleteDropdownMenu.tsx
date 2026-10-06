import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

const ConnectedUserSheetDeleteDropdownMenu = ({onDeleteClick}: {onDeleteClick: () => void}) => (
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
            <DropdownMenuItem className="dropdown-menu-item-destructive" onClick={onDeleteClick} variant="destructive">
                <Trash2Icon /> Delete
            </DropdownMenuItem>
        </DropdownMenuContent>
    </DropdownMenu>
);

export default ConnectedUserSheetDeleteDropdownMenu;
