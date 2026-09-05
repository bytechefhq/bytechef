import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface DataPillPanelStateI {
    dataPillPanelHasContent: boolean;
    dataPillPanelOpen: boolean;
    isDraggingDataPill: boolean;
    setDataPillPanelHasContent: (dataPillPanelHasContent: boolean) => void;
    setDataPillPanelOpen: (dataPillPanelOpen: boolean) => void;
    setIsDraggingDataPill: (isDraggingDataPill: boolean) => void;
}

const useDataPillPanelStore = create<DataPillPanelStateI>()(
    devtools(
        (set) => ({
            dataPillPanelHasContent: false,
            dataPillPanelOpen: false,
            isDraggingDataPill: false,
            setDataPillPanelHasContent: (dataPillPanelHasContent) =>
                set((state) => ({...state, dataPillPanelHasContent})),
            setDataPillPanelOpen: (dataPillPanelOpen) => set((state) => ({...state, dataPillPanelOpen})),
            setIsDraggingDataPill: (isDraggingDataPill) => set((state) => ({...state, isDraggingDataPill})),
        }),
        {
            name: 'data-pill-panel',
        }
    )
);

export default useDataPillPanelStore;
