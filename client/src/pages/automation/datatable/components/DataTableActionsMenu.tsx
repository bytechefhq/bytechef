import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {DownloadIcon, MoreVerticalIcon, PencilIcon, Trash2Icon, UploadIcon} from 'lucide-react';

interface DataTableActionsMenuProps {
    onDeleteTable: () => void;
    onExportCsv: () => void;
    onImportCsv: () => void;
    onRenameTable: () => void;
    tableId?: string;
}

const DataTableActionsMenu = ({
    onDeleteTable,
    onExportCsv,
    onImportCsv,
    onRenameTable,
    tableId,
}: DataTableActionsMenuProps) => {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button aria-label="More actions" icon={<MoreVerticalIcon className="h-4 w-4" />} variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem icon={<UploadIcon />} label="Import CSV" onClick={onImportCsv} />

                <DropdownMenuItem icon={<DownloadIcon />} label="Export CSV" onClick={onExportCsv} />

                {tableId && <DropdownMenuItem icon={<PencilIcon />} label="Rename Table" onClick={onRenameTable} />}

                {tableId && (
                    <>
                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            icon={<Trash2Icon />}
                            label="Delete Table"
                            onClick={onDeleteTable}
                            variant="destructive"
                        />
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableActionsMenu;
