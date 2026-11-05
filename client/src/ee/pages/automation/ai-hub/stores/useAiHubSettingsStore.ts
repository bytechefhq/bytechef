import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface AiHubSettingsStateI {
    artifactsCardCollapsed: boolean;
    setArtifactsCardCollapsed: (artifactsCardCollapsed: boolean) => void;
    setShowToolCalls: (showToolCalls: boolean) => void;
    showToolCalls: boolean;
}

const useAiHubSettingsStore = create<AiHubSettingsStateI>()(
    devtools(
        persist(
            (set) => ({
                artifactsCardCollapsed: false,

                setArtifactsCardCollapsed: (artifactsCardCollapsed) => set({artifactsCardCollapsed}),

                setShowToolCalls: (showToolCalls) => set({showToolCalls}),

                showToolCalls: false,
            }),
            {
                name: 'bytechef.ai-hub-settings',
            }
        )
    )
);

export default useAiHubSettingsStore;
