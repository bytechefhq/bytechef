import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DuplicateDataTableDialogStateI {
    baseName: string;
    clearTableToDuplicate: () => void;
    duplicateValue: string;
    setDuplicateValue: (value: string) => void;
    setTableToDuplicate: (tableId: string, baseName: string) => void;
    tableIdToDuplicate: string | null;
}

export const useDuplicateDataTableDialogStore = create<DuplicateDataTableDialogStateI>()(
    devtools(
        (set) => ({
            baseName: '',

            clearTableToDuplicate: () => {
                set(() => ({
                    baseName: '',
                    duplicateValue: '',
                    tableIdToDuplicate: null,
                }));
            },

            duplicateValue: '',

            setDuplicateValue: (value: string) => {
                set(() => ({
                    duplicateValue: value,
                }));
            },

            setTableToDuplicate: (tableId: string, baseName: string) => {
                set(() => ({
                    baseName,
                    duplicateValue: `${baseName}_copy`,
                    tableIdToDuplicate: tableId,
                }));
            },

            tableIdToDuplicate: null,
        }),
        {
            name: 'bytechef.duplicate-datatable-dialog',
        }
    )
);
