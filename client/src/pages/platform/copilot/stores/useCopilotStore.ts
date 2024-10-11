import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface CopilotStateI {
    showCopilot: boolean;
    setShowCopilot: (showCopilot: boolean) => void;
}

export const useCopilotStore = create<CopilotStateI>()(
    devtools(
        persist(
            (set) => ({
                setShowCopilot: (showCopilot) =>
                    set((state) => {
                        return {
                            ...state,
                            showCopilot,
                        };
                    }),
                showCopilot: false,
            }),
            {
                name: 'bytechef.copilot',
            }
        )
    )
);
