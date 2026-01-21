import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import useDataTableListItemDropdownMenu from '@/pages/automation/datatables/components/hooks/useDataTableListItemDropdownMenu';
import {CopyIcon, DownloadIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface DataTableListItemDropdownMenuProps {
    baseName: string;
    dataTableId: string;
}

const DataTableListItemDropdownMenu = ({baseName, dataTableId}: DataTableListItemDropdownMenuProps) => {
    const {handleDeleteClick, handleDuplicateClick, handleExportCsvClick, handleRenameClick} =
        useDataTableListItemDropdownMenu({
            baseName,
            dataTableId,
        });

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button aria-label="Table menu" icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem className="dropdown-menu-item" onClick={handleRenameClick}>
                    <EditIcon /> Rename
                </DropdownMenuItem>

                <DropdownMenuItem className="dropdown-menu-item" onClick={handleDuplicateClick}>
                    <CopyIcon /> Duplicate
                </DropdownMenuItem>

                <DropdownMenuItem className="dropdown-menu-item" onClick={handleExportCsvClick}>
                    <DownloadIcon /> Export CSV
                </DropdownMenuItem>

                <DropdownMenuSeparator className="m-0" />

                <DropdownMenuItem className="dropdown-menu-item-destructive" onClick={handleDeleteClick}>
                    <Trash2Icon /> Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableListItemDropdownMenu;
