import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {cleanupForChatChange} from '../AiHubRuntimeProvider';

/**
 * Pins the chat-switch cleanup contract — and crucially, what cleanup must NOT do.
 *
 * Earlier shapes of {@link cleanupForChatChange} called {@code controller.abort()} on the
 * in-flight workflow stream as defense-in-depth against late events bleeding into the new chat.
 * That had a user-visible regression: switching chats mid-stream killed the agent run server-
 * side, so returning to the original chat later showed the user message with no assistant
 * reply forever (the run was terminated and never committed). The bleed it was guarding against is
 * already prevented by the chat-id guards on the subscriber's event handlers.
 *
 * This test file pins the new contract: cleanup ONLY resets local tool-call UI state; the stream
 * keeps running so its committed final message can be loaded on return via {@code switchChat}'s
 * server refetch. Provider-unmount handles the abort separately (in a different effect in the
 * provider component, not this helper).
 */
describe('cleanupForChatChange', () => {
    beforeEach(() => {
        aiChatToolCallStore.setState({toolCalls: {}});
        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    it('calls resetForChat with the previousChatId snapshot', () => {
        const resetSpy = vi.spyOn(aiChatToolCallStore.getState(), 'resetForChat');

        cleanupForChatChange('conv-A');

        // The contract is "reset entries belonging to the chat we are LEAVING" — so a future
        // refactor that passed the *new* chatId would silently delete tool-call cards that had
        // just arrived for the new chat. This assertion pins the snapshot direction.
        expect(resetSpy).toHaveBeenCalledWith('conv-A');
        expect(resetSpy).toHaveBeenCalledTimes(1);
    });

    it('passes undefined to resetForChat when the previous id is undefined', () => {
        // Initial-mount path: previousChatId is undefined on the first effect run.
        // resetForChat must accept undefined per its signature; without this test a future
        // tightening of the parameter to `string` would compile but blow up at runtime on every first
        // switch.
        const resetSpy = vi.spyOn(aiChatToolCallStore.getState(), 'resetForChat');

        cleanupForChatChange(undefined);

        expect(resetSpy).toHaveBeenCalledWith(undefined);
    });

    /*
     * Pinned regression: the helper MUST NOT call failAllRunning. Earlier shapes did, marking every
     * in-flight tool-call card as `aborted` on chat switch. That short-circuited the server-
     * side stream's lifecycle from the UI's perspective even though the run was still going (the
     * server-side abort was happening separately via the controller). With the abort removed (so the
     * run can finish in the background and surface on return via the message refetch), keeping
     * failAllRunning here would mark cards as terminal-aborted while the underlying run actually
     * succeeds — the user would return to "completed message + aborted cards" which is contradictory.
     */
    it('does NOT mark running tool calls as aborted on chat switch', () => {
        const failAllRunningSpy = vi.spyOn(aiChatToolCallStore.getState(), 'failAllRunning');

        cleanupForChatChange('conv-A');

        expect(failAllRunningSpy).not.toHaveBeenCalled();
    });

    /*
     * Pinned regression: the helper signature DOES NOT take an AbortController ref anymore. The
     * provider's per-turn AbortController is INTENTIONALLY left running on chat switch so the
     * agent run completes in the background and the committed assistant message becomes visible on
     * return. The provider has a separate unmount-only effect that aborts on real page exit.
     *
     * This test exists as a structural guard: a refactor that re-introduced the controller arg would
     * fail the type-check at the call sites in the provider, but having a callable-shape assertion
     * here makes the API surface change deliberately visible to a code reviewer.
     */
    it('takes only previousChatId — no AbortController argument', () => {
        // Calling with a single argument must satisfy TypeScript's signature; if a refactor adds back
        // a required ref this won't compile.
        cleanupForChatChange('conv-Z');

        // No assertions on store mutations beyond resetForChat; the absence of failAllRunning
        // / abort calls is covered above and via the missing controller arg here.
        expect(true).toBe(true);
    });
});
