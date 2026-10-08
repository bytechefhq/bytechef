import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CopilotPanelStateI {
    buildModeDisabled: boolean;
    copilotPanelOpen: boolean;
    setBuildModeDisabled: (buildModeDisabled: boolean) => void;
    setCopilotPanelOpen: (open: boolean) => void;
}

const useCopilotPanelStore = create<CopilotPanelStateI>()(
    devtools((set) => ({
        buildModeDisabled: false,
        copilotPanelOpen: false,
        setBuildModeDisabled: (buildModeDisabled) => set({buildModeDisabled}),
        setCopilotPanelOpen: (copilotPanelOpen) => set({copilotPanelOpen}),
    }))
);

export default useCopilotPanelStore;
