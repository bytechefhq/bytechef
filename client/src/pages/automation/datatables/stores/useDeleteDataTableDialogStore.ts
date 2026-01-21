import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DeleteDataTableDialogStateI {
    clearTableIdToDelete: () => void;
    setTableIdToDelete: (tableId: string) => void;
    tableIdToDelete: string | null;
}

export const useDeleteDataTableDialogStore = create<DeleteDataTableDialogStateI>()(
    devtools(
        (set) => ({
            clearTableIdToDelete: () => {
                set(() => ({
                    tableIdToDelete: null,
                }));
            },

            setTableIdToDelete: (tableId: string) => {
                set(() => ({
                    tableIdToDelete: tableId,
                }));
            },

            tableIdToDelete: null,
        }),
        {
            name: 'bytechef.delete-datatable-dialog',
        }
    )
);
