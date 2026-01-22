import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface AddDataTableColumnDialogStateI {
    clearDialog: () => void;
    open: boolean;
    setOpen: (open: boolean) => void;
}

export const useAddDataTableColumnDialogStore = create<AddDataTableColumnDialogStateI>()(
    devtools(
        (set) => ({
            clearDialog: () => {
                set(() => ({open: false}));
            },

            open: false,

            setOpen: (open: boolean) => {
                set(() => ({open}));
            },
        }),
        {
            name: 'bytechef.datatable-add-column-dialog',
        }
    )
);
