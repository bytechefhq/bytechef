import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
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

    const canCreateDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_CREATE');
    const canDeleteDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_DELETE');
    const canEditDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_EDIT');

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
                {canEditDataTable && (
                    <DropdownMenuItem icon={<EditIcon />} label="Rename" onClick={handleRenameClick} />
                )}

                {canCreateDataTable && (
                    <DropdownMenuItem icon={<CopyIcon />} label="Duplicate" onClick={handleDuplicateClick} />
                )}

                <DropdownMenuItem icon={<DownloadIcon />} label="Export CSV" onClick={handleExportCsvClick} />

                {canDeleteDataTable && (
                    <>
                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            icon={<Trash2Icon />}
                            label="Delete"
                            onClick={handleDeleteClick}
                            variant="destructive"
                        />
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableListItemDropdownMenu;
