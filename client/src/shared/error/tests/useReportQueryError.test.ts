import {renderHook} from '@testing-library/react';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {reportMutationError, useReportQueryError} from '../useReportQueryError';

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

describe('useReportQueryError', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
    });

    it('toasts once for the same error across re-renders', () => {
        const error = new Error('boom');

        const {rerender} = renderHook(({currentError}) => useReportQueryError('Load items', currentError), {
            initialProps: {currentError: error as Error | null},
        });

        rerender({currentError: error});
        rerender({currentError: new Error('boom')});

        expect(toast.error).toHaveBeenCalledTimes(1);
        expect(toast.error).toHaveBeenCalledWith('boom');
    });

    it('toasts again for a new message', () => {
        const {rerender} = renderHook(({currentError}) => useReportQueryError('Load items', currentError), {
            initialProps: {currentError: new Error('first') as Error | null},
        });

        rerender({currentError: new Error('second')});

        expect(toast.error).toHaveBeenCalledTimes(2);
        expect(toast.error).toHaveBeenLastCalledWith('second');
    });

    it('does not toast when the error is null', () => {
        renderHook(() => useReportQueryError('Load items', null));

        expect(toast.error).not.toHaveBeenCalled();
    });

    it('toasts again when the same message returns after the error cleared', () => {
        const {rerender} = renderHook(({currentError}) => useReportQueryError('Load items', currentError), {
            initialProps: {currentError: new Error('boom') as Error | null},
        });

        rerender({currentError: null});
        rerender({currentError: new Error('boom')});

        expect(toast.error).toHaveBeenCalledTimes(2);
    });
});

describe('reportMutationError', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
    });

    it('toasts the error message', () => {
        reportMutationError('Delete item', new Error('nope'));

        expect(toast.error).toHaveBeenCalledWith('nope');
    });

    it('falls back to the action name when the message is empty', () => {
        reportMutationError('Delete item', new Error(''));

        expect(toast.error).toHaveBeenCalledWith('Delete item failed');
    });
});
