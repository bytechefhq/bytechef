import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
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
            <DropdownMenuItem icon={<Trash2Icon />} label="Delete" onClick={onDeleteClick} variant="destructive" />
        </DropdownMenuContent>
    </DropdownMenu>
);

export default ConnectedUserSheetDeleteDropdownMenu;
