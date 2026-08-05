import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface AiHubSettingsStateI {
    setShowToolCalls: (showToolCalls: boolean) => void;
    showToolCalls: boolean;
}

/**
 * User-level AI Hub chat display preferences. Tool-call cards are hidden by default — the transcript reads
 * as a conversation, with the interactive cards (askUserQuestion, artifact links) still shown — and the
 * thread header toggle flips them on for debugging/inspection. Persisted so the choice survives reloads.
 */
const useAiHubSettingsStore = create<AiHubSettingsStateI>()(
    devtools(
        persist(
            (set) => ({
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
