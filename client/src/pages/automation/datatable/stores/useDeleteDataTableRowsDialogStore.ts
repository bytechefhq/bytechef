import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DeleteDataTableRowsDialogStateI {
    clearDialog: () => void;
    open: boolean;
    setOpen: () => void;
}

export const useDeleteDataTableRowsDialogStore = create<DeleteDataTableRowsDialogStateI>()(
    devtools(
        (set) => ({
            clearDialog: () => {
                set(() => ({open: false}));
            },

            open: false,

            setOpen: () => {
                set(() => ({open: true}));
            },
        }),
        {
            name: 'bytechef.datatable-delete-rows-dialog',
        }
    )
);
