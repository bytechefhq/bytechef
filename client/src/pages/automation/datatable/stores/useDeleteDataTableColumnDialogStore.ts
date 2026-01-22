import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DeleteDataTableColumnDialogStateI {
    clearDialog: () => void;
    columnId: string | null;
    columnName: string | null;
    setColumnToDelete: (columnId: string, columnName: string) => void;
}

export const useDeleteDataTableColumnDialogStore = create<DeleteDataTableColumnDialogStateI>()(
    devtools(
        (set) => ({
            clearDialog: () => {
                set(() => ({columnId: null, columnName: null}));
            },

            columnId: null,

            columnName: null,

            setColumnToDelete: (columnId: string, columnName: string) => {
                set(() => ({columnId, columnName}));
            },
        }),
        {
            name: 'bytechef.datatable-delete-column-dialog',
        }
    )
);
