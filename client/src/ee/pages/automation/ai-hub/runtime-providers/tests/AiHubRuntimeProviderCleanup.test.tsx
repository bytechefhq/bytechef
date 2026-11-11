import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {cleanupForChatChange} from '../AiHubRuntimeProvider';

/**
 * Pins the chat-switch cleanup contract: cleanup only resets the local tool-call UI state and the retry banner of the
 * chat being left.
 * It must not abort the run or mark running tool calls aborted, because the run keeps going server-side and its
 * committed reply is loaded when the user returns to the chat.
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

    it('drops the retry banner of the chat being left', () => {
        aiChatRetryableErrorStore.getState().setError({
            errorMessage: 'Component slack/v9 does not exist',
            lastUserMessage: 'Build a Slack workflow',
            toolName: 'buildWorkflow',
        });

        cleanupForChatChange('conv-A');

        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
    });
});
