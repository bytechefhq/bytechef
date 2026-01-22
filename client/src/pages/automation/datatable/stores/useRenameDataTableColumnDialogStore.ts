import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface RenameDataTableColumnDialogStateI {
    clearDialog: () => void;
    columnId: string | null;
    currentName: string;
    renameValue: string;
    setColumnToRename: (columnId: string, currentName: string) => void;
    setRenameValue: (value: string) => void;
}

export const useRenameDataTableColumnDialogStore = create<RenameDataTableColumnDialogStateI>()(
    devtools(
        (set) => ({
            clearDialog: () => {
                set(() => ({
                    columnId: null,
                    currentName: '',
                    renameValue: '',
                }));
            },

            columnId: null,

            currentName: '',

            renameValue: '',

            setColumnToRename: (columnId: string, currentName: string) => {
                set(() => ({
                    columnId,
                    currentName,
                    renameValue: currentName,
                }));
            },

            setRenameValue: (value: string) => {
                set(() => ({renameValue: value}));
            },
        }),
        {
            name: 'bytechef.datatable-rename-column-dialog',
        }
    )
);
