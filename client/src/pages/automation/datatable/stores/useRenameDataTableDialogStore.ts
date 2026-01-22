import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface RenameDataTableDialogStateI {
    baseName: string;
    clearTableToRename: () => void;
    renameValue: string;
    setRenameValue: (value: string) => void;
    setTableToRename: (tableId: string, baseName: string) => void;
    tableIdToRename: string | null;
}

export const useRenameDataTableDialogStore = create<RenameDataTableDialogStateI>()(
    devtools(
        (set) => ({
            baseName: '',

            clearTableToRename: () => {
                set(() => ({
                    baseName: '',
                    renameValue: '',
                    tableIdToRename: null,
                }));
            },

            renameValue: '',

            setRenameValue: (value: string) => {
                set(() => ({renameValue: value}));
            },

            setTableToRename: (tableId: string, baseName: string) => {
                set(() => ({
                    baseName,
                    renameValue: baseName,
                    tableIdToRename: tableId,
                }));
            },

            tableIdToRename: null,
        }),
        {
            name: 'bytechef.datatable-rename-dialog',
        }
    )
);
