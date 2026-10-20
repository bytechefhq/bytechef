import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {EditIcon, EllipsisVerticalIcon} from 'lucide-react';
import {ReactNode} from 'react';

interface ConnectionListItemDropdownMenuProps {
    children: ReactNode;
    onEditClick: () => void;
}

const ConnectionListItemDropdownMenu = ({children, onEditClick}: ConnectionListItemDropdownMenuProps) => (
    <DropdownMenu>
        <DropdownMenuTrigger asChild>
            <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
        </DropdownMenuTrigger>

        <DropdownMenuContent align="end">
            <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />

            <DropdownMenuSeparator />

            {children}
        </DropdownMenuContent>
    </DropdownMenu>
);

export default ConnectionListItemDropdownMenu;
