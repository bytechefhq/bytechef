import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CopilotStateI {
    showCopilot: boolean;
    setShowCopilot: (showCopilot: boolean) => void;
}

export const useCopilotStore = create<CopilotStateI>()(
    devtools(
        (set) => ({
            setShowCopilot: (showCopilot) =>
                set(() => {
                    if (showCopilot) {
                        window.CommandBar.openCopilot();
                    } else {
                        window.CommandBar.closeHelpHub();
                    }

                    return {
                        showCopilot,
                    };
                }),
            showCopilot: false,
        }),
        {
            name: 'copilot',
        }
    )
);
