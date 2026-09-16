import {renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

const {toast} = await import('sonner');

import {useReportQueryError} from '../useReportQueryError';

const renderReportQueryError = (initialError: Error | null) =>
    renderHook(({error}: {error: Error | null}) => useReportQueryError('List memories', error), {
        initialProps: {error: initialError},
    });

beforeEach(() => {
    vi.mocked(toast.error).mockClear();

    vi.spyOn(console, 'error').mockImplementation(() => {});
});

afterEach(() => {
    vi.restoreAllMocks();
});

describe('useReportQueryError', () => {
    it('does not toast while there is no error', () => {
        renderReportQueryError(null);

        expect(toast.error).not.toHaveBeenCalled();
    });

    it('toasts a message once even when the same error is observed again', () => {
        const {rerender} = renderReportQueryError(new Error('boom'));

        rerender({error: new Error('boom')});

        expect(toast.error).toHaveBeenCalledTimes(1);
        expect(toast.error).toHaveBeenCalledWith('boom');
        expect(console.error).toHaveBeenCalledWith('List memories failed:', expect.any(Error));
    });

    it('toasts again once the error has cleared and comes back', () => {
        const {rerender} = renderReportQueryError(new Error('boom'));

        rerender({error: null});
        rerender({error: new Error('boom')});

        expect(toast.error).toHaveBeenCalledTimes(2);
    });

    it('toasts each distinct message', () => {
        const {rerender} = renderReportQueryError(new Error('boom'));

        rerender({error: new Error('bang')});

        expect(toast.error).toHaveBeenNthCalledWith(1, 'boom');
        expect(toast.error).toHaveBeenNthCalledWith(2, 'bang');
    });

    it('falls back to the action when the message is empty', () => {
        renderReportQueryError(new Error(''));

        expect(toast.error).toHaveBeenCalledWith('List memories failed');
    });

    it('falls back to the action when the error carries no message', () => {
        const error = new Error();

        Object.defineProperty(error, 'message', {value: undefined});

        renderReportQueryError(error);

        expect(toast.error).toHaveBeenCalledWith('List memories failed');
    });
});
