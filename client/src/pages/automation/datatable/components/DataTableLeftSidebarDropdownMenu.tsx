import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
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

                <DropdownMenuItem
                    className="text-red-600 focus:text-red-700"
                    onSelect={() => handleDeleteOpen(tableId, tableName)}
                >
                    <Trash2 className="mr-2 size-4" /> Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableLeftSidebarDropdownMenu;
