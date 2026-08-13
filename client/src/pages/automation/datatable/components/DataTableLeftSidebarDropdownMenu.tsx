import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {MoreVertical, Pencil, Trash2} from 'lucide-react';

import useDeleteDataTableAlertDialog from '../hooks/useDeleteDataTableAlertDialog';
import useRenameDataTableDialog from '../hooks/useRenameDataTableDialog';

interface Props {
    tableId: string;
    tableName: string;
}

const DataTableLeftSidebarDropdownMenu = ({tableId, tableName}: Props) => {
    const {handleOpen: handleDeleteOpen} = useDeleteDataTableAlertDialog();
    const {handleOpen: handleRenameOpen} = useRenameDataTableDialog();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label="Table menu"
                    className="w-6 opacity-0 transition-opacity group-hover:opacity-100 data-[state=open]:opacity-100"
                    icon={<MoreVertical className="h-4" />}
                    size="iconSm"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onSelect={() => handleRenameOpen(tableId, tableName)}>
                    <Pencil className="mr-2 size-4" /> Rename
                </DropdownMenuItem>

                <DropdownMenuSeparator />

                {/* variant rather than hand-rolled red-* classes: it colours the icon too (the item's own
                    muted-svg rule wins over a colour class on the item), carries the destructive focus
                    background, and picks up the dark-mode pair without spelling them out. */}

                <DropdownMenuItem onSelect={() => handleDeleteOpen(tableId, tableName)} variant="destructive">
                    <Trash2 className="mr-2 size-4" /> Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableLeftSidebarDropdownMenu;
