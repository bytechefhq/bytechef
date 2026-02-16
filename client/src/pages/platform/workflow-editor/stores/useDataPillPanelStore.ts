import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DataPillPanelStateI {
    dataPillPanelOpen: boolean;
    isDraggingDataPill: boolean;
    setDataPillPanelOpen: (dataPillPanelOpen: boolean) => void;
    setIsDraggingDataPill: (isDraggingDataPill: boolean) => void;
}

const useDataPillPanelStore = create<DataPillPanelStateI>()(
    devtools(
        (set) => ({
            dataPillPanelOpen: false,
            isDraggingDataPill: false,
            setDataPillPanelOpen: (dataPillPanelOpen) => set((state) => ({...state, dataPillPanelOpen})),
            setIsDraggingDataPill: (isDraggingDataPill) => set((state) => ({...state, isDraggingDataPill})),
        }),
        {
            name: 'data-pill-panel',
        }
    )
);

export default useDataPillPanelStore;
