import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DeleteDataTableDialogStateI {
    clearTableToDelete: () => void;
    setTableToDelete: (tableId: string, tableName: string) => void;
    tableIdToDelete: string | null;
    tableNameToDelete: string | null;
}

export const useDeleteDataTableDialogStore = create<DeleteDataTableDialogStateI>()(
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
