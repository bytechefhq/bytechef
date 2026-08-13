import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';

export interface UseOpenCopilotOptionsI {
    // Guiding placeholder text shown in the composer for this turn (e.g. "Describe what the workflow should
    // do"). Falls back to the composer's generic placeholder when omitted.
    composerPlaceholder?: string;
    mode?: MODE;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: Record<string, any>;
    source: Source;
}

/**
 * Returns a function that opens the Copilot panel for a given surface. Every caller — the CopilotButton
 * trigger and, once BUILD/ASK menu items exist, dropdown menu items that can't reuse the button component —
 * shares this same "open" behavior so the fresh-context contract stays in one place.
 */
const useOpenCopilot = () => {
    const setContext = useCopilotStore((state) => state.setContext);
    const setComposerPlaceholder = useCopilotStore((state) => state.setComposerPlaceholder);
    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);

    return ({composerPlaceholder, mode = MODE.ASK, parameters = {}, source}: UseOpenCopilotOptionsI) => {
        const {generateConversationId, resetMessages, saveConversationState, setGlobalPanelConversationToken} =
            useCopilotStore.getState();

        // Push, then clear: the surface being opened starts a genuinely new conversation, and the one it
        // covers stays recoverable on the stack. Without the push the previous surface's messages would be
        // replayed against this surface's agent, since both the agent id and the /ai/chat/{source} URL derive
        // from the context installed just below. Order matches the seven local-panel surfaces.
        //
        // Only push when the panel is currently closed. When it is already open, this call is replacing one
        // global-panel conversation with another (e.g. a per-row Copilot action on a listing page, clicked
        // for project A and then project B) — there is nothing new to cover, and pushing a second time would
        // leak an entry that the one eventual close can never pop back to.
        if (!copilotPanelOpen) {
            const token = saveConversationState();

            setGlobalPanelConversationToken(token);
        }

        resetMessages();
        generateConversationId();

        // A fresh context, not a spread of the previous one: switching surfaces must drop the prior surface's
        // parameters and workflowExecutionError rather than leaking them into this agent's state.
        setContext({
            mode,
            parameters,
            source,
        });

        // Fresh open, not a merge: a caller that doesn't pass a placeholder must clear one left behind by a
        // previous surface rather than inheriting it.
        setComposerPlaceholder(composerPlaceholder);

        setCopilotPanelOpen(true);
    };
};

export default useOpenCopilot;
