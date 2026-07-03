import {beforeEach, describe, expect, it} from 'vitest';

import {aiChatRetryableErrorStore} from '../useAiChatRetryableErrorStore';

describe('useAiChatRetryableErrorStore', () => {
    beforeEach(() => {
        aiChatRetryableErrorStore.setState({currentError: undefined});
    });

    it('starts with currentError undefined', () => {
        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
    });

    it('setError stores the retryable error', () => {
        aiChatRetryableErrorStore.getState().setError({
            errorMessage: 'File not found',
            lastUserMessage: 'Create the report',
            toolName: 'createFile',
        });

        const {currentError} = aiChatRetryableErrorStore.getState();

        expect(currentError).not.toBeUndefined();
        expect(currentError!.errorMessage).toBe('File not found');
        expect(currentError!.lastUserMessage).toBe('Create the report');
        expect(currentError!.toolName).toBe('createFile');
    });

    it('clearError removes the stored error', () => {
        aiChatRetryableErrorStore.getState().setError({
            errorMessage: 'Something went wrong',
            lastUserMessage: 'Do the thing',
            toolName: 'doThing',
        });

        aiChatRetryableErrorStore.getState().clearError();

        expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
    });
});
