/* eslint-disable sort-keys */
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DataPillPanelStateI {
    dataPillPanelOpen: boolean;
    setDataPillPanelOpen: (dataPillPanelOpen: boolean) => void;
}

export const useDataPillPanelStore = create<DataPillPanelStateI>()(
    devtools(
        (set) => ({
            dataPillPanelOpen: false,
            setDataPillPanelOpen: (dataPillPanelOpen) => set((state) => ({...state, dataPillPanelOpen})),
        }),
        {
            name: 'data-pill-panel',
        }
    )
);
