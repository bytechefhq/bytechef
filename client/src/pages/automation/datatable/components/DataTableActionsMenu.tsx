import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
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
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    // Dropping a table is ADMIN-tier on the server (DataTableGraphQlController.dropDataTable), so offering it to a member
    // without DATA_TABLE_DELETE means a confirm dialog that ends in a 403.
    const canDeleteDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_DELETE');
    const canEditDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_EDIT');

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button aria-label="More actions" icon={<MoreVerticalIcon className="h-4 w-4" />} variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                {canEditDataTable && (
                    <DropdownMenuItem icon={<UploadIcon />} label="Import CSV" onClick={onImportCsv} />
                )}

                <DropdownMenuItem icon={<DownloadIcon />} label="Export CSV" onClick={onExportCsv} />

                {tableId && canEditDataTable && (
                    <DropdownMenuItem icon={<PencilIcon />} label="Rename Table" onClick={onRenameTable} />
                )}

                {tableId && canDeleteDataTable && (
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
