import {ColumnType, useAddDataTableColumnMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';

import {useAddDataTableColumnDialogStore} from '../stores/useAddDataTableColumnDialogStore';
import {useCurrentDataTableStore} from '../stores/useCurrentDataTableStore';

interface UseAddDataTableColumnDialogI {
    handleAdd: (name: string, type: ColumnType) => void;
    handleClose: () => void;
    handleOpen: () => void;
    handleOpenChange: (open: boolean) => void;
    open: boolean;
}

export default function useAddDataTableColumnDialog(): UseAddDataTableColumnDialogI {
    const {clearDialog, open, setOpen} = useAddDataTableColumnDialogStore();
    const {dataTable} = useCurrentDataTableStore();
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const addColumnMutation = useAddDataTableColumnMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['dataTables']});
            clearDialog();
        },
    });

    const handleClose = () => {
        clearDialog();
    };

    const handleOpen = () => {
        setOpen(true);
    };

    const handleAdd = (name: string, type: ColumnType) => {
        if (!dataTable?.id) return;

        addColumnMutation.mutate({
            input: {column: {name, type}, environmentId: String(environmentId), tableId: dataTable.id},
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
    };
}
