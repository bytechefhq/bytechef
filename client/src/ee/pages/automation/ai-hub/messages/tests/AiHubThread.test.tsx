import {shouldShowThreadLoadingState} from '@/ee/pages/automation/ai-hub/messages/AiHubThread';
import {describe, expect, it} from 'vitest';

describe('shouldShowThreadLoadingState', () => {
    it('shows the loading skeleton while history is being fetched (front of a switch)', () => {
        // switchChat clears the store to [] and flips messagesLoading true before the fetch resolves.
        expect(shouldShowThreadLoadingState(true, false)).toBe(true);
    });

    it('shows the loading skeleton at the tail of a switch when the store already holds the fetched messages', () => {
        // The assistant-ui runtime ingests messages in a useEffect, so it still reports "empty" for one
        // commit after aiHubStore.messages is populated and messagesLoading has flipped false. Bridging that
        // frame with the skeleton is what prevents the welcome copy (identical to the home page) from flashing.
        expect(shouldShowThreadLoadingState(false, true)).toBe(true);
    });

    it('shows the welcome prompt for a genuinely empty new chat (no history, nothing loading)', () => {
        // A freshly created workflow chat: messages=[], messagesLoading=false. This is
        // the one legitimate welcome case and must not be masked by the skeleton.
        expect(shouldShowThreadLoadingState(false, false)).toBe(false);
    });
});
