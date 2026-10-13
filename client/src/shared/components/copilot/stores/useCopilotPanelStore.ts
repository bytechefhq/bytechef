import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CopilotPanelStateI {
    compactHeader: boolean;
    copilotPanelOpen: boolean;
    setCompactHeader: (compactHeader: boolean) => void;
    setCopilotPanelOpen: (open: boolean) => void;
}

const useCopilotPanelStore = create<CopilotPanelStateI>()(
    devtools((set) => ({
        compactHeader: false,
        copilotPanelOpen: false,
        setCompactHeader: (compactHeader) => set({compactHeader}),
        setCopilotPanelOpen: (copilotPanelOpen) => set({copilotPanelOpen}),
    }))
);

export default useCopilotPanelStore;
