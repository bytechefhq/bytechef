import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DeleteDataTableAlertDialogStateI {
    clearTableToDelete: () => void;
    setTableToDelete: (tableId: string, tableName: string) => void;
    tableIdToDelete: string | null;
    tableNameToDelete: string | null;
}

export const useDeleteDataTableAlertDialogStore = create<DeleteDataTableAlertDialogStateI>()(
    devtools(
        (set) => ({
            clearTableToDelete: () => {
                set(() => ({
                    tableIdToDelete: null,
                    tableNameToDelete: null,
                }));
            },

            setTableToDelete: (tableId: string, tableName: string) => {
                set(() => ({
                    tableIdToDelete: tableId,
                    tableNameToDelete: tableName,
                }));
            },

            tableIdToDelete: null,
            tableNameToDelete: null,
        }),
        {
            name: 'bytechef.datatable-delete-dialog',
        }
    )
);
