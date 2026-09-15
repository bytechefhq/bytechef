import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CopilotPanelStateI {
    buildModeDisabled: boolean;
    compactHeader: boolean;
    copilotPanelOpen: boolean;
    setBuildModeDisabled: (buildModeDisabled: boolean) => void;
    setCompactHeader: (compactHeader: boolean) => void;
    setCopilotPanelOpen: (open: boolean) => void;
}

const useCopilotPanelStore = create<CopilotPanelStateI>()(
    devtools((set) => ({
        buildModeDisabled: false,
        compactHeader: false,
        copilotPanelOpen: false,
        setBuildModeDisabled: (buildModeDisabled) => set({buildModeDisabled}),
        setCompactHeader: (compactHeader) => set({compactHeader}),
        setCopilotPanelOpen: (copilotPanelOpen) => set({copilotPanelOpen}),
    }))
);

export default useCopilotPanelStore;
