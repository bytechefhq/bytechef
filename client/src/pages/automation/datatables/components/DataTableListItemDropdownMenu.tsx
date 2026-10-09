import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
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
                <DropdownMenuItem icon={<EditIcon />} label="Rename" onClick={handleRenameClick} />

                <DropdownMenuItem icon={<CopyIcon />} label="Duplicate" onClick={handleDuplicateClick} />

                <DropdownMenuItem icon={<DownloadIcon />} label="Export CSV" onClick={handleExportCsvClick} />

                <DropdownMenuSeparator />

                <DropdownMenuItem
                    icon={<Trash2Icon />}
                    label="Delete"
                    onClick={handleDeleteClick}
                    variant="destructive"
                />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableListItemDropdownMenu;
