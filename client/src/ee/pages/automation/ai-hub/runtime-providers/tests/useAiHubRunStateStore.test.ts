import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubRunStateStore, isChatRunning} from '../stores/useAiHubRunStateStore';

describe('useAiHubRunStateStore', () => {
    beforeEach(() => {
        aiHubRunStateStore.getState().reset();
    });

    it('marks a chat running and reports it via isChatRunning', () => {
        aiHubRunStateStore.getState().setChatRunning('thread-a', true);

        expect(isChatRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(true);
    });

    it('does not leak one chat running state into another chat', () => {
        aiHubRunStateStore.getState().setChatRunning('thread-b', true);

        expect(isChatRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(false);
    });

    it('clears a chat running state without affecting other chats', () => {
        aiHubRunStateStore.getState().setChatRunning('thread-a', true);
        aiHubRunStateStore.getState().setChatRunning('thread-b', true);
        aiHubRunStateStore.getState().setChatRunning('thread-a', false);

        expect(isChatRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(false);
        expect(isChatRunning(aiHubRunStateStore.getState(), 'thread-b')).toBe(true);
    });

    it('treats a chat with an in-flight workflow stream as running', () => {
        aiHubRunStateStore.getState().adjustInflightStreamCount('thread-a', 1);

        expect(isChatRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(true);
    });

    it('clamps the in-flight stream count at zero', () => {
        aiHubRunStateStore.getState().adjustInflightStreamCount('thread-a', 1);
        aiHubRunStateStore.getState().adjustInflightStreamCount('thread-a', -1);
        aiHubRunStateStore.getState().adjustInflightStreamCount('thread-a', -1);

        expect(isChatRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(false);
        expect(aiHubRunStateStore.getState().inflightStreamCountByChat['thread-a']).toBe(0);
    });

    it('keeps a chat running while its workflow stream is open even after the agent run ends', () => {
        aiHubRunStateStore.getState().setChatRunning('thread-a', true);
        aiHubRunStateStore.getState().adjustInflightStreamCount('thread-a', 1);
        aiHubRunStateStore.getState().setChatRunning('thread-a', false);

        expect(isChatRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(true);
    });

    it('stores a per-chat runId for the cancel mutation', () => {
        aiHubRunStateStore.getState().setChatRunId('thread-a', 'run-1');

        expect(aiHubRunStateStore.getState().runIdByChat['thread-a']).toBe('run-1');
    });

    it('returns not-running for an undefined chat id', () => {
        expect(isChatRunning(aiHubRunStateStore.getState(), undefined)).toBe(false);
    });

    it('ignores writes for an undefined chat id', () => {
        aiHubRunStateStore.getState().setChatRunning(undefined, true);
        aiHubRunStateStore.getState().adjustInflightStreamCount(undefined, 1);

        expect(aiHubRunStateStore.getState().runningByChat).toEqual({});
        expect(aiHubRunStateStore.getState().inflightStreamCountByChat).toEqual({});
    });
});
