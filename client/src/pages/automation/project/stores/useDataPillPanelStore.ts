/* eslint-disable sort-keys */
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DataPillPanelState {
    dataPillPanelOpen: boolean;
    setDataPillPanelOpen: (dataPillPanelOpen: boolean) => void;
}

export const useDataPillPanelStore = create<DataPillPanelState>()(
    devtools(
        (set) => ({
            dataPillPanelOpen: false,
            setDataPillPanelOpen: (dataPillPanelOpen) =>
                set((state) => ({...state, dataPillPanelOpen})),
        }),
        {
            name: 'data-pill-panel',
        }
    )
);
