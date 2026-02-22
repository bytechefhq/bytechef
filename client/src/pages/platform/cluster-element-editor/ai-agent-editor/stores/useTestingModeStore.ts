import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface TestingModeStateI {
    isTestingAgent: boolean;
    jobKey: string | null;
    resetTestingMode: () => void;
    setIsTestingAgent: (isTesting: boolean) => void;
    setJobKey: (jobKey: string | null) => void;
}

export const useTestingModeStore = create<TestingModeStateI>()(
    devtools(
        (set) => ({
            isTestingAgent: false,

            jobKey: null,

            resetTestingMode: () => {
                set(() => ({
                    isTestingAgent: false,
                    jobKey: null,
                }));
            },

            setIsTestingAgent: (isTesting: boolean) => {
                set(() => ({isTestingAgent: isTesting}));
            },

            setJobKey: (jobKey: string | null) => {
                set(() => ({jobKey}));
            },
        }),
        {
            name: 'bytechef.ai-agent-testing',
        }
    )
);
