import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubRunStateStore, isTaskRunning} from '../stores/useAiHubRunStateStore';

describe('useAiHubRunStateStore', () => {
    beforeEach(() => {
        aiHubRunStateStore.getState().reset();
    });

    it('marks a task running and reports it via isTaskRunning', () => {
        aiHubRunStateStore.getState().setTaskRunning('thread-a', true);

        expect(isTaskRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(true);
    });

    it('does not leak one task running state into another task', () => {
        aiHubRunStateStore.getState().setTaskRunning('thread-b', true);

        expect(isTaskRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(false);
    });

    it('clears a task running state without affecting other tasks', () => {
        aiHubRunStateStore.getState().setTaskRunning('thread-a', true);
        aiHubRunStateStore.getState().setTaskRunning('thread-b', true);
        aiHubRunStateStore.getState().setTaskRunning('thread-a', false);

        expect(isTaskRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(false);
        expect(isTaskRunning(aiHubRunStateStore.getState(), 'thread-b')).toBe(true);
    });

    it('treats a task with an in-flight workflow stream as running', () => {
        aiHubRunStateStore.getState().adjustInflightStreamCount('thread-a', 1);

        expect(isTaskRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(true);
    });

    it('clamps the in-flight stream count at zero', () => {
        aiHubRunStateStore.getState().adjustInflightStreamCount('thread-a', 1);
        aiHubRunStateStore.getState().adjustInflightStreamCount('thread-a', -1);
        aiHubRunStateStore.getState().adjustInflightStreamCount('thread-a', -1);

        expect(isTaskRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(false);
        expect(aiHubRunStateStore.getState().inflightStreamCountByTask['thread-a']).toBe(0);
    });

    it('keeps a task running while its workflow stream is open even after the agent run ends', () => {
        aiHubRunStateStore.getState().setTaskRunning('thread-a', true);
        aiHubRunStateStore.getState().adjustInflightStreamCount('thread-a', 1);
        aiHubRunStateStore.getState().setTaskRunning('thread-a', false);

        expect(isTaskRunning(aiHubRunStateStore.getState(), 'thread-a')).toBe(true);
    });

    it('stores a per-task runId for the cancel mutation', () => {
        aiHubRunStateStore.getState().setTaskRunId('thread-a', 'run-1');

        expect(aiHubRunStateStore.getState().runIdByTask['thread-a']).toBe('run-1');
    });

    it('returns not-running for an undefined task id', () => {
        expect(isTaskRunning(aiHubRunStateStore.getState(), undefined)).toBe(false);
    });

    it('ignores writes for an undefined task id', () => {
        aiHubRunStateStore.getState().setTaskRunning(undefined, true);
        aiHubRunStateStore.getState().adjustInflightStreamCount(undefined, 1);

        expect(aiHubRunStateStore.getState().runningByTask).toEqual({});
        expect(aiHubRunStateStore.getState().inflightStreamCountByTask).toEqual({});
    });
});
