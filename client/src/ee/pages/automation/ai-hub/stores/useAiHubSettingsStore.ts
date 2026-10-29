import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface AiHubSettingsStateI {
    artifactsCardCollapsed: boolean;
    setArtifactsCardCollapsed: (artifactsCardCollapsed: boolean) => void;
    setShowToolCalls: (showToolCalls: boolean) => void;
    showToolCalls: boolean;
}

/**
 * User-level AI Hub chat display preferences. Tool-call cards are hidden by default (the interactive cards stay
 * visible) and the thread header toggle flips them on. The Artifacts card collapsed state is global rather than per
 * chat. Both are persisted so the choice survives reloads.
 */
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
