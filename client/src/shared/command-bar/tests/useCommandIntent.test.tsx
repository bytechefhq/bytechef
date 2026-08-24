import {useCommandIntent} from '@/shared/command-bar/useCommandIntent';
import {useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

describe('useCommandIntent', () => {
    beforeEach(() => {
        useCommandIntentStore.getState().reset();
    });

    it('runs the handler with the payload when the intent matches', () => {
        const handler = vi.fn();

        useCommandIntentStore.getState().publish('project.create', {id: 7});

        renderHook(() => useCommandIntent('project.create', handler));

        expect(handler).toHaveBeenCalledWith({id: 7});
    });

    it('does not run the handler when no intent is pending', () => {
        const handler = vi.fn();

        renderHook(() => useCommandIntent('project.create', handler));

        expect(handler).not.toHaveBeenCalled();
    });

    it('runs the handler only once when two components claim the same key', () => {
        const first = vi.fn();
        const second = vi.fn();

        useCommandIntentStore.getState().publish('dataTable.create');

        renderHook(() => useCommandIntent('dataTable.create', first));
        renderHook(() => useCommandIntent('dataTable.create', second));

        expect(first).toHaveBeenCalledTimes(1);
        expect(second).not.toHaveBeenCalled();
    });

    it('does not claim the intent when disabled, leaving it pending for another claimant', () => {
        const handler = vi.fn();

        useCommandIntentStore.getState().publish('project.create');

        renderHook(() => useCommandIntent('project.create', handler, false));

        expect(handler).not.toHaveBeenCalled();
        expect(useCommandIntentStore.getState().intent).toEqual({key: 'project.create', payload: undefined});
    });

    it('claims the intent when explicitly enabled', () => {
        const handler = vi.fn();

        useCommandIntentStore.getState().publish('project.create', {id: 9});

        renderHook(() => useCommandIntent('project.create', handler, true));

        expect(handler).toHaveBeenCalledWith({id: 9});
    });

    it('claims an intent published while the claimant is already mounted, on the same route', () => {
        // A create command runs [navigate(page), intent(key)]. Navigating to the route the user is already on does
        // not remount the route element, so the already-mounted dialog's claim must come from reacting to the
        // store, not from a mount-only effect -- otherwise "Cmd+K -> Create project" while already standing on the
        // Projects page would be a silent no-op.
        const handler = vi.fn();

        renderHook(() => useCommandIntent('project.create', handler));

        expect(handler).not.toHaveBeenCalled();

        act(() => {
            useCommandIntentStore.getState().publish('project.create', {id: 11});
        });

        expect(handler).toHaveBeenCalledWith({id: 11});
        expect(handler).toHaveBeenCalledTimes(1);
    });
});
