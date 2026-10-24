import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import DataSyncDialog from '@/pages/automation/data-syncs/components/DataSyncDialog';
import DeleteDataSyncAlertDialog from '@/pages/automation/data-syncs/components/DeleteDataSyncAlertDialog';
import useDataSyncActions from '@/pages/automation/data-syncs/hooks/useDataSyncActions';
import {MoreVerticalIcon, PencilIcon, Trash2Icon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

interface DataSyncsLeftSidebarDropdownMenuProps {
    current: boolean;
    dataSync: {description?: string | null; id: string; title: string};
}

const DataSyncsLeftSidebarDropdownMenu = ({current, dataSync}: DataSyncsLeftSidebarDropdownMenuProps) => {
    const navigate = useNavigate();

    const {
        deleteDataSync,
        handleDeleteClick,
        isDeleting,
        openEditDialog,
        setShowDeleteConfirmDialog,
        setShowEditDialog,
        showDeleteConfirmDialog,
        showEditDialog,
    } = useDataSyncActions({
        dataSync,
        onDeleted: () => {
            if (current) {
                navigate('/automation/projects?dataSyncs=all');
            }
        },
    });

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label={`${dataSync.title} menu`}
                        className="w-6 opacity-0 transition-opacity group-hover:opacity-100 data-[state=open]:opacity-100"
                        icon={<MoreVerticalIcon className="h-4" />}
                        size="iconSm"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onSelect={openEditDialog}>
                        <PencilIcon className="mr-2 size-4" /> Edit
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem disabled={isDeleting} onSelect={handleDeleteClick} variant="destructive">
                        <Trash2Icon className="mr-2 size-4" /> Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <DataSyncDialog dataSync={dataSync} onOpenChange={setShowEditDialog} open={showEditDialog} />

            {showDeleteConfirmDialog && (
                <DeleteDataSyncAlertDialog
                    dataSyncTitle={dataSync.title}
                    onClose={() => setShowDeleteConfirmDialog(false)}
                    onDelete={deleteDataSync}
                />
            )}
        </>
    );
};

export default DataSyncsLeftSidebarDropdownMenu;
