import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface AiHubSettingsStateI {
    artifactsCardCollapsed: boolean;
    artifactsCardHidden: boolean;
    setArtifactsCardCollapsed: (artifactsCardCollapsed: boolean) => void;
    setArtifactsCardHidden: (artifactsCardHidden: boolean) => void;
}

const useAiHubSettingsStore = create<AiHubSettingsStateI>()(
    devtools(
        persist(
            (set) => ({
                artifactsCardCollapsed: false,
                artifactsCardHidden: false,

                setArtifactsCardCollapsed: (artifactsCardCollapsed) => set({artifactsCardCollapsed}),

                setArtifactsCardHidden: (artifactsCardHidden) => set({artifactsCardHidden}),
            }),
            {
                name: 'bytechef.ai-hub-settings',
            }
        )
    )
);

export default useAiHubSettingsStore;
