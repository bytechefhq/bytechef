/* eslint-disable sort-keys */
import {create} from 'zustand';

interface DataPillPanelState {
    dataPillPanelOpen: boolean;
    setDataPillPanelOpen: (dataPillPanelOpen: boolean) => void;
}

export const useDataPillPanelStore = create<DataPillPanelState>()((set) => ({
    dataPillPanelOpen: false,

    setDataPillPanelOpen: (dataPillPanelOpen) =>
        set((state) => ({...state, dataPillPanelOpen})),
}));
