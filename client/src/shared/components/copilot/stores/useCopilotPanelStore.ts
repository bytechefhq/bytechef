import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CopilotPanelStateI {
    copilotPanelOpen: boolean;
    setCopilotPanelOpen: (open: boolean) => void;
}

const useCopilotPanelStore = create<CopilotPanelStateI>()(
    devtools((set) => ({
        copilotPanelOpen: false,
        setCopilotPanelOpen: (copilotPanelOpen) => set({copilotPanelOpen}),
    }))
);

export default useCopilotPanelStore;
