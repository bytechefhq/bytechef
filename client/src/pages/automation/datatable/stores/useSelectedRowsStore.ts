import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface SelectedRowsStateI {
    clearSelectedRows: () => void;
    selectedRows: ReadonlySet<string>;
    setSelectedRows: (rows: ReadonlySet<string>) => void;
}

export const useSelectedRowsStore = create<SelectedRowsStateI>()(
    devtools(
        (set) => ({
            clearSelectedRows: () => {
                set(() => ({selectedRows: new Set<string>()}));
            },

            selectedRows: new Set<string>(),

            setSelectedRows: (rows: ReadonlySet<string>) => {
                set(() => ({selectedRows: rows}));
            },
        }),
        {
            name: 'bytechef.datatable-selected-rows',
        }
    )
);
