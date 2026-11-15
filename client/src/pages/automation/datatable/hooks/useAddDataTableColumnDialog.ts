import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    ColumnLinkInput,
    ColumnType,
    DataTable,
    useAddDataTableColumnMutation,
    useDataTablesQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';

import {useAddDataTableColumnDialogStore} from '../stores/useAddDataTableColumnDialogStore';
import {useCurrentDataTableStore} from '../stores/useCurrentDataTableStore';

interface UseAddDataTableColumnDialogI {
    handleAdd: (name: string, type: ColumnType, link?: ColumnLinkInput) => void;
    handleClose: () => void;
    handleOpen: () => void;
    handleOpenChange: (open: boolean) => void;
    open: boolean;
    targetTables: DataTable[];
}

export default function useAddDataTableColumnDialog(): UseAddDataTableColumnDialogI {
    const {clearDialog, open, setOpen} = useAddDataTableColumnDialogStore();
    const {dataTable} = useCurrentDataTableStore();
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: tablesData} = useDataTablesQuery({
        environmentId: String(environmentId),
        workspaceId: String(workspaceId),
    });

    const targetTables = (tablesData?.dataTables ?? []).filter((table) => table.id !== dataTable?.id) as DataTable[];

    const addColumnMutation = useAddDataTableColumnMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['dataTables']});
            clearDialog();
        },
    });

    const handleClose = () => {
        clearDialog();
    };

    const handleOpen = () => {
        setOpen(true);
    };

    const handleAdd = (name: string, type: ColumnType, link?: ColumnLinkInput) => {
        if (!dataTable?.id) return;

        addColumnMutation.mutate({
            input: {
                column: {...(link ? {link} : {}), name, type},
                environmentId: String(environmentId),
                tableId: dataTable.id,
            },
        });
    };

    const handleOpenChange = (openValue: boolean) => {
        if (!openValue) {
            handleClose();
        } else {
            setOpen(true);
        }
    };

    return {
        handleAdd,
        handleClose,
        handleOpen,
        handleOpenChange,
        open,
        targetTables,
    };
}
