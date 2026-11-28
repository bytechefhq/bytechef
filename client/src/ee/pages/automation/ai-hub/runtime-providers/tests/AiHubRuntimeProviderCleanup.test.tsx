import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {cleanupForChatChange} from '../AiHubRuntimeProvider';

describe('cleanupForChatChange', () => {
    beforeEach(() => {
        aiChatToolCallStore.setState({toolCalls: {}});
        vi.restoreAllMocks();
        vi.clearAllMocks();
    });

    it('calls resetForChat with the previousChatId snapshot', () => {
        const resetSpy = vi.spyOn(aiChatToolCallStore.getState(), 'resetForChat');

        cleanupForChatChange('conv-A');

        expect(resetSpy).toHaveBeenCalledWith('conv-A');
        expect(resetSpy).toHaveBeenCalledTimes(1);
    });

    it('passes undefined to resetForChat when the previous id is undefined', () => {
        const resetSpy = vi.spyOn(aiChatToolCallStore.getState(), 'resetForChat');

        cleanupForChatChange(undefined);

        expect(resetSpy).toHaveBeenCalledWith(undefined);
    });

    it('does NOT mark running tool calls as aborted on chat switch', () => {
        const failAllRunningSpy = vi.spyOn(aiChatToolCallStore.getState(), 'failAllRunning');

        cleanupForChatChange('conv-A');

        expect(failAllRunningSpy).not.toHaveBeenCalled();
    });
});
