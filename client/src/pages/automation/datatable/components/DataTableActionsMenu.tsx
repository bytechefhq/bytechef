import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
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
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    // Dropping a table is ADMIN-tier on the server (DataTableGraphQlController.dropDataTable), so offering it to a member
    // without DATA_TABLE_DELETE means a confirm dialog that ends in a 403.
    const canDeleteDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_DELETE');
    const canEditDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_EDIT');

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button aria-label="More actions" icon={<MoreVertical className="h-4 w-4" />} variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                {canEditDataTable && (
                    <DropdownMenuItem onClick={onImportCsv}>
                        <Upload className="mr-2 size-4" /> Import CSV
                    </DropdownMenuItem>
                )}

                <DropdownMenuItem onClick={onExportCsv}>
                    <Download className="mr-2 h-4 w-4" /> Export CSV
                </DropdownMenuItem>

                {tableId && canEditDataTable && (
                    <DropdownMenuItem onClick={onRenameTable}>
                        <Pencil className="mr-2 h-4 w-4" /> Rename Table
                    </DropdownMenuItem>
                )}

                {tableId && canDeleteDataTable && (
                    <>
                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            className="text-content-destructive focus:text-content-destructive-primary"
                            onClick={onDeleteTable}
                        >
                            <Trash2 className="mr-2 h-4 w-4" /> Delete Table
                        </DropdownMenuItem>
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableActionsMenu;
