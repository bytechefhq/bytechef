import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {MoreVerticalIcon, PencilIcon, Trash2Icon} from 'lucide-react';

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
                    icon={<MoreVerticalIcon className="h-4" />}
                    size="iconSm"
                    variant="ghost"
                />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem
                    icon={<PencilIcon />}
                    label="Rename"
                    onSelect={() => handleRenameOpen(tableId, tableName)}
                />

                <DropdownMenuItem
                    icon={<Trash2Icon />}
                    label="Delete"
                    onSelect={() => handleDeleteOpen(tableId, tableName)}
                    variant="destructive"
                />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableLeftSidebarDropdownMenu;
