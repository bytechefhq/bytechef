import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Download, MoreVertical, Pencil, Trash2, Upload} from 'lucide-react';

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
                <Button aria-label="More actions" icon={<MoreVertical className="h-4 w-4" />} variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={onImportCsv}>
                    <Upload className="mr-2 size-4" /> Import CSV
                </DropdownMenuItem>

                <DropdownMenuItem onClick={onExportCsv}>
                    <Download className="mr-2 h-4 w-4" /> Export CSV
                </DropdownMenuItem>

                {tableId && (
                    <DropdownMenuItem onClick={onRenameTable}>
                        <Pencil className="mr-2 h-4 w-4" /> Rename Table
                    </DropdownMenuItem>
                )}

                {tableId && (
                    <DropdownMenuItem
                        className="text-content-destructive focus:text-content-destructive-primary"
                        onClick={onDeleteTable}
                    >
                        <Trash2 className="mr-2 h-4 w-4" /> Delete Table
                    </DropdownMenuItem>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableActionsMenu;
