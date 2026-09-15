import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import useDataTableListItemDropdownMenu from '@/pages/automation/datatables/components/hooks/useDataTableListItemDropdownMenu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {CopyIcon, DownloadIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface DataTableListItemDropdownMenuProps {
    baseName: string;
    dataTableId: string;
}

const DataTableListItemDropdownMenu = ({baseName, dataTableId}: DataTableListItemDropdownMenuProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {handleDeleteClick, handleDuplicateClick, handleExportCsvClick, handleRenameClick} =
        useDataTableListItemDropdownMenu({
            baseName,
            dataTableId,
        });
    const canCreateDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_CREATE');
    const canDeleteDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_DELETE');
    const canEditDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_EDIT');

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button aria-label="Table menu" icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                {canEditDataTable && (
                    <DropdownMenuItem className="dropdown-menu-item" onClick={handleRenameClick}>
                        <EditIcon /> Rename
                    </DropdownMenuItem>
                )}

                {canCreateDataTable && (
                    <DropdownMenuItem className="dropdown-menu-item" onClick={handleDuplicateClick}>
                        <CopyIcon /> Duplicate
                    </DropdownMenuItem>
                )}

                <DropdownMenuItem className="dropdown-menu-item" onClick={handleExportCsvClick}>
                    <DownloadIcon /> Export CSV
                </DropdownMenuItem>

                {canDeleteDataTable && (
                    <>
                        <DropdownMenuSeparator className="m-0" />

                        <DropdownMenuItem
                            className="dropdown-menu-item-destructive"
                            onClick={handleDeleteClick}
                            variant="destructive"
                        >
                            <Trash2Icon /> Delete
                        </DropdownMenuItem>
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableListItemDropdownMenu;
