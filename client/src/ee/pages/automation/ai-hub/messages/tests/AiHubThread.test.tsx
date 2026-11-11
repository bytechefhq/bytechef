import {shouldShowThreadLoadingState} from '@/ee/pages/automation/ai-hub/messages/AiHubThread';
import {describe, expect, it} from 'vitest';

describe('shouldShowThreadLoadingState', () => {
    it('shows the loading state while history is being fetched (front of a switch)', () => {
        expect(shouldShowThreadLoadingState(true, false)).toBe(true);
    });

    it('shows the loading state at the tail of a switch when the store already holds the fetched messages', () => {
        expect(shouldShowThreadLoadingState(false, true)).toBe(true);
    });

    it('shows the welcome prompt for a genuinely empty new chat', () => {
        expect(shouldShowThreadLoadingState(false, false)).toBe(false);
    });
});
