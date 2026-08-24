import {UNCLAIMED_INTENT_LIFETIME, useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

describe('useCommandIntentStore', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        useCommandIntentStore.getState().reset();
    });

    afterEach(() => {
        vi.useRealTimers();

        // vi.spyOn(console, 'warn') returns the SAME spy across tests when the method is already spied,
        // so its call count leaks into the next test unless restored here.
        vi.restoreAllMocks();
    });

    it('returns the intent to the matching claimant', () => {
        useCommandIntentStore.getState().publish('project.create', {id: 7});

        expect(useCommandIntentStore.getState().claim('project.create')).toEqual({
            key: 'project.create',
            payload: {id: 7},
        });
    });

    it('returns undefined to a non-matching claimant and leaves the intent pending', () => {
        useCommandIntentStore.getState().publish('project.create');

        expect(useCommandIntentStore.getState().claim('dataTable.create')).toBeUndefined();
        expect(useCommandIntentStore.getState().intent).toBeDefined();
    });

    it('clears the intent synchronously so a second claimant finds nothing', () => {
        useCommandIntentStore.getState().publish('project.create');

        useCommandIntentStore.getState().claim('project.create');

        expect(useCommandIntentStore.getState().claim('project.create')).toBeUndefined();
    });

    it('warns when an intent goes unclaimed', () => {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});

        useCommandIntentStore.getState().publish('project.create');

        vi.advanceTimersByTime(UNCLAIMED_INTENT_LIFETIME);

        expect(warn).toHaveBeenCalledWith(expect.stringContaining('project.create'));
    });

    it('does not warn when the intent was claimed in time', () => {
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});

        useCommandIntentStore.getState().publish('project.create');
        useCommandIntentStore.getState().claim('project.create');

        vi.advanceTimersByTime(UNCLAIMED_INTENT_LIFETIME);

        expect(warn).not.toHaveBeenCalled();
    });

    it('expires the intent once its lifetime elapses, so it is gone from state', () => {
        useCommandIntentStore.getState().publish('project.create');

        vi.advanceTimersByTime(UNCLAIMED_INTENT_LIFETIME);

        expect(useCommandIntentStore.getState().intent).toBeUndefined();
    });

    it('returns undefined to a claim attempted after the intent has expired', () => {
        useCommandIntentStore.getState().publish('project.create');

        vi.advanceTimersByTime(UNCLAIMED_INTENT_LIFETIME);

        expect(useCommandIntentStore.getState().claim('project.create')).toBeUndefined();
    });
});
