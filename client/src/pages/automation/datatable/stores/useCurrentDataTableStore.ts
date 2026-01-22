import {DataTable} from '@/shared/middleware/graphql';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CurrentDataTableStateI {
    clearDataTable: () => void;
    dataTable: DataTable | undefined;
    setDataTable: (dataTable: DataTable | undefined) => void;
}

export const useCurrentDataTableStore = create<CurrentDataTableStateI>()(
    devtools(
        (set) => ({
            clearDataTable: () => {
                set(() => ({dataTable: undefined}));
            },

            dataTable: undefined,

            setDataTable: (dataTable: DataTable | undefined) => {
                set(() => ({dataTable}));
            },
        }),
        {
            name: 'bytechef.datatable-current',
        }
    )
);
