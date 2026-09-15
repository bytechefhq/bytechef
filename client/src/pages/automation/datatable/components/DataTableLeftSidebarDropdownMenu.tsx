import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {MoreVertical, Pencil, Trash2} from 'lucide-react';

import useDeleteDataTableAlertDialog from '../hooks/useDeleteDataTableAlertDialog';
import useRenameDataTableDialog from '../hooks/useRenameDataTableDialog';

interface Props {
    tableId: string;
    tableName: string;
}

const DataTableLeftSidebarDropdownMenu = ({tableId, tableName}: Props) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {handleOpen: handleDeleteOpen} = useDeleteDataTableAlertDialog();
    const {handleOpen: handleRenameOpen} = useRenameDataTableDialog();
    const canDeleteDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_DELETE');
    const canEditDataTable = useHasWorkspaceScope(currentWorkspaceId, 'DATA_TABLE_EDIT');

    if (!canEditDataTable && !canDeleteDataTable) {
        return null;
    }

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
                {canEditDataTable && (
                    <DropdownMenuItem onSelect={() => handleRenameOpen(tableId, tableName)}>
                        <Pencil className="mr-2 size-4" /> Rename
                    </DropdownMenuItem>
                )}

                {canDeleteDataTable && (
                    <DropdownMenuItem
                        className="text-red-600 focus:text-red-700"
                        onSelect={() => handleDeleteOpen(tableId, tableName)}
                    >
                        <Trash2 className="mr-2 size-4" /> Delete
                    </DropdownMenuItem>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DataTableLeftSidebarDropdownMenu;
