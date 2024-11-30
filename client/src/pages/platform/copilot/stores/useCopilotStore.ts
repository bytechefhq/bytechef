import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface CopilotStateI {
    copilotPanelOpen: boolean;
    setCopilotPanelOpen: (showCopilot: boolean) => void;
}

export const useCopilotStore = create<CopilotStateI>()(
    devtools(
        persist(
            (set) => ({
                copilotPanelOpen: false,
                setCopilotPanelOpen: (copilotPanelOpen) =>
                    set((state) => {
                        return {
                            ...state,
                            copilotPanelOpen,
                        };
                    }),
            }),
            {
                name: 'bytechef.copilot-panel',
            }
        )
    )
);
