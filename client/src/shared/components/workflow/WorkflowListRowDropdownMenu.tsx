import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {DownloadIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {ReactNode} from 'react';

interface WorkflowListRowDropdownMenuProps {
    children?: ReactNode;
    exportUrl: string;
    onDeleteClick: () => void;
    onEditClick: () => void;
}

const WorkflowListRowDropdownMenu = ({
    children,
    exportUrl,
    onDeleteClick,
    onEditClick,
}: WorkflowListRowDropdownMenuProps) => (
    <DropdownMenu>
        <DropdownMenuTrigger asChild>
            <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
        </DropdownMenuTrigger>

        <DropdownMenuContent align="end">
            <DropdownMenuItem icon={<EditIcon />} label="Edit" onClick={onEditClick} />

            {children}

            <DropdownMenuItem
                icon={<DownloadIcon />}
                label="Export"
                onClick={() => (window.location.href = exportUrl)}
            />

            <DropdownMenuSeparator />

            <DropdownMenuItem icon={<Trash2Icon />} label="Delete" onClick={onDeleteClick} variant="destructive" />
        </DropdownMenuContent>
    </DropdownMenu>
);

export default WorkflowListRowDropdownMenu;
