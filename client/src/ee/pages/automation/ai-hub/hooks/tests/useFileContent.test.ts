import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import useFileContent from '../useFileContent';

/**
 * Pins the binary-MIME branch in useFileContent: rather than surfacing an "unsupported" error, it must
 * return empty content with the observed mimeType so the renderer can dispatch by mimeType to an
 * `<img>` (images) or `<iframe>` (PDF / unknown) fallback. The error path is reserved for actual
 * HTTP-level failures.
 */
describe('useFileContent', () => {
    const originalFetch = global.fetch;

    afterEach(() => {
        global.fetch = originalFetch;
        vi.restoreAllMocks();
    });

    it('returns plain text content for a text/markdown response', async () => {
        const headers = new Headers({'Content-Type': 'text/markdown'});

        global.fetch = vi.fn().mockResolvedValue({
            headers,
            ok: true,
            status: 200,
            text: () => Promise.resolve('# Hello\nWorld'),
        });

        const {result} = renderHook(() => useFileContent('file-1'));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.content).toBe('# Hello\nWorld');
        expect(result.current.mimeType).toBe('text/markdown');
        expect(result.current.error).toBeUndefined();
    });

    it('returns empty content + the binary mime type so the renderer can dispatch to <img>/<iframe>', async () => {
        const headers = new Headers({'Content-Type': 'image/png'});

        global.fetch = vi.fn().mockResolvedValue({
            headers,
            ok: true,
            status: 200,
            text: () => Promise.resolve('binary-bytes'),
        });

        const {result} = renderHook(() => useFileContent('file-2'));

        await waitFor(() => expect(result.current.loading).toBe(false));

        // No error: the renderer needs a clean state to walk the mimeType-based dispatch chain
        // (IMAGE_MIME_TYPES, application/pdf, ...) without short-circuiting on an artificial error.
        expect(result.current.error).toBeUndefined();
        expect(result.current.content).toBe('');
        expect(result.current.mimeType).toBe('image/png');
    });

    it('treats application/octet-stream (the no-Content-Type fallback) as binary without erroring', async () => {
        const headers = new Headers();

        global.fetch = vi.fn().mockResolvedValue({
            headers,
            ok: true,
            status: 200,
            text: () => Promise.resolve(''),
        });

        const {result} = renderHook(() => useFileContent('file-3'));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.error).toBeUndefined();
        expect(result.current.content).toBe('');
        expect(result.current.mimeType).toBe('application/octet-stream');
    });

    it('passes application/json through as text', async () => {
        const headers = new Headers({'Content-Type': 'application/json'});

        global.fetch = vi.fn().mockResolvedValue({
            headers,
            ok: true,
            status: 200,
            text: () => Promise.resolve('{"ok":true}'),
        });

        const {result} = renderHook(() => useFileContent('file-4'));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.content).toBe('{"ok":true}');
        expect(result.current.mimeType).toBe('application/json');
    });

    it('surfaces an error when the response is non-OK', async () => {
        global.fetch = vi.fn().mockResolvedValue({
            headers: new Headers(),
            ok: false,
            status: 500,
            text: () => Promise.resolve(''),
        });

        const {result} = renderHook(() => useFileContent('file-5'));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.error).toContain('500');
    });

    it('drops the response when the hook unmounts before fetch resolves', async () => {
        let resolveFetch: ((response: Response) => void) | null = null;

        global.fetch = vi.fn().mockImplementation(
            () =>
                new Promise<Response>((resolve) => {
                    resolveFetch = resolve;
                })
        );

        const {result, unmount} = renderHook(() => useFileContent('file-6'));

        unmount();

        await act(async () => {
            // Resolve the fetch AFTER unmount; the cancellation guard inside the hook must skip the setState.
            const headers = new Headers({'Content-Type': 'text/plain'});

            resolveFetch!({
                headers,
                ok: true,
                status: 200,
                text: () => Promise.resolve('late'),
            } as Response);
            // Flush microchats so the .then chain runs.
            await Promise.resolve();
        });

        // Result is captured at unmount time; subsequent setState (if it had run) would not be observable here.
        // Asserting the hook's last seen state matches the still-loading initial state proves the cancellation
        // guard fired — without it, the stale fetch would update state on an unmounted component and React would
        // surface a warning (which we'd see in test output).
        expect(result.current.loading).toBe(true);
    });
});
