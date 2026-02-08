import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface ClusterElementsCanvasDialogStateI {
    copilotPanelOpen: boolean;
}

interface ClusterElementsCanvasDialogActionsI {
    reset: () => void;
    setCopilotPanelOpen: (open: boolean) => void;
}

type ClusterElementsCanvasDialogStoreType = ClusterElementsCanvasDialogActionsI & ClusterElementsCanvasDialogStateI;

const initialState: ClusterElementsCanvasDialogStateI = {
    copilotPanelOpen: false,
};

export const useClusterElementsCanvasDialogStore = create<ClusterElementsCanvasDialogStoreType>()(
    devtools(
        (set) => ({
            ...initialState,
            reset: () => set(initialState),
            setCopilotPanelOpen: (open) => set({copilotPanelOpen: open}),
        }),
        {name: 'bytechef.cluster-elements-canvas-dialog-store'}
    )
);
