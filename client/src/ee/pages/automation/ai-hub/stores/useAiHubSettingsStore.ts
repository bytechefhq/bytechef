import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface AiHubSettingsStateI {
    artifactsCardCollapsed: boolean;
    setArtifactsCardCollapsed: (artifactsCardCollapsed: boolean) => void;
}

const useAiHubSettingsStore = create<AiHubSettingsStateI>()(
    devtools(
        persist(
            (set) => ({
                artifactsCardCollapsed: false,

                setArtifactsCardCollapsed: (artifactsCardCollapsed) => set({artifactsCardCollapsed}),
            }),
            {
                name: 'bytechef.ai-hub-settings',
            }
        )
    )
);

export default useAiHubSettingsStore;
