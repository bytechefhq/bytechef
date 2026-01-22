import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface ImportDataTableCsvDialogStateI {
    clearDialog: () => void;
    open: boolean;
    setOpen: (open: boolean) => void;
}

export const useImportDataTableCsvDialogStore = create<ImportDataTableCsvDialogStateI>()(
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
            name: 'bytechef.datatable-import-csv-dialog',
        }
    )
);
