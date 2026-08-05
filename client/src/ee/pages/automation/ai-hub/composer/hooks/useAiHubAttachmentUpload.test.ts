import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, test, vi} from 'vitest';

import {ALLOWED_MIME_TYPES, useAiHubAttachmentUpload} from './useAiHubAttachmentUpload';

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

const {toast} = await import('sonner');

const mockToast = vi.mocked(toast);

const mockOkResponse = (overrides: Partial<{id: number; mimeType: string; name: string; sizeBytes: number}> = {}) =>
    ({
        json: async () => ({
            id: 7,
            mimeType: 'image/png',
            name: 'screenshot.png',
            sizeBytes: 1024,
            ...overrides,
        }),
        ok: true,
        status: 201,
    }) as Response;

describe('useAiHubAttachmentUpload', () => {
    beforeEach(() => {
        aiHubComposerStore.setState({referencedResources: []});

        global.fetch = vi.fn().mockResolvedValue(mockOkResponse());
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    test('uploads an allowed file and adds a reference to the composer store', async () => {
        const {result} = renderHook(() => useAiHubAttachmentUpload(42));

        const file = new File(['data'], 'screenshot.png', {type: 'image/png'});

        await act(async () => {
            result.current.upload([file]);
        });

        await waitFor(() => {
            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(1);
            expect(referencedResources[0]).toMatchObject({id: '7', kind: 'file', name: 'screenshot.png'});
        });

        expect(global.fetch).toHaveBeenCalledWith(
            '/api/automation/internal/asset-files/upload',
            expect.objectContaining({body: expect.any(FormData), method: 'POST'})
        );
    });

    test('rejects unsupported mime types via toast and skips the request', async () => {
        const {result} = renderHook(() => useAiHubAttachmentUpload(42));

        const file = new File(['data'], 'archive.zip', {type: 'application/zip'});

        act(() => {
            result.current.upload([file]);
        });

        expect(mockToast.error).toHaveBeenCalledWith(expect.stringMatching(/File type not supported/));
        expect(global.fetch).not.toHaveBeenCalled();
    });

    test('routes a text-mime file through the multipart endpoint', async () => {
        const {result} = renderHook(() => useAiHubAttachmentUpload(42));

        const file = new File(['# spec'], 'spec.md', {type: 'text/markdown'});

        global.fetch = vi.fn().mockResolvedValue(mockOkResponse({id: 9, mimeType: 'text/markdown', name: 'spec.md'}));

        await act(async () => {
            result.current.upload([file]);
        });

        await waitFor(() => {
            expect(global.fetch).toHaveBeenCalledTimes(1);
        });

        const [, init] = (global.fetch as ReturnType<typeof vi.fn>).mock.calls[0]!;
        const formData = init.body as FormData;

        expect(formData.get('workspaceId')).toBe('42');
        expect(formData.get('file')).toBe(file);
    });

    test('marks an upload as error when the server fails and surfaces the message', async () => {
        global.fetch = vi.fn().mockResolvedValue({
            json: async () => ({message: 'Quota exceeded: workspace has only 100 bytes free'}),
            ok: false,
            status: 413,
            statusText: 'Payload Too Large',
        });

        const {result} = renderHook(() => useAiHubAttachmentUpload(42));

        const file = new File(['data'], 'big.pdf', {type: 'application/pdf'});

        await act(async () => {
            result.current.upload([file]);
        });

        await waitFor(() => {
            expect(result.current.uploads.find((upload) => upload.file === file)?.status).toBe('error');
        });

        expect(mockToast.error).toHaveBeenCalledWith('Quota exceeded: workspace has only 100 bytes free');
    });

    test('retry re-triggers the upload after a failure', async () => {
        const failOnce = vi
            .fn()
            .mockResolvedValueOnce({
                json: async () => ({message: 'transient error'}),
                ok: false,
                status: 500,
                statusText: 'Server Error',
            })
            .mockResolvedValueOnce(mockOkResponse({id: 11, mimeType: 'image/png', name: 'pic.png'}));

        global.fetch = failOnce;

        const {result} = renderHook(() => useAiHubAttachmentUpload(42));

        const file = new File(['data'], 'pic.png', {type: 'image/png'});

        await act(async () => {
            result.current.upload([file]);
        });

        await waitFor(() => {
            expect(result.current.uploads.find((upload) => upload.file === file)?.status).toBe('error');
        });

        await act(async () => {
            result.current.retry(file);
        });

        await waitFor(() => {
            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toContainEqual({id: '11', kind: 'file', name: 'pic.png'});
        });

        expect(failOnce).toHaveBeenCalledTimes(2);
    });

    test('records error status when workspaceId is missing', async () => {
        const {result} = renderHook(() => useAiHubAttachmentUpload(undefined));

        const file = new File(['data'], 'screenshot.png', {type: 'image/png'});

        await act(async () => {
            result.current.upload([file]);
        });

        await waitFor(() => {
            expect(result.current.uploads.find((upload) => upload.file === file)?.status).toBe('error');
        });

        expect(global.fetch).not.toHaveBeenCalled();
    });

    test('exposes the canonical mime allow-list including the new pdf and gif types', () => {
        expect(ALLOWED_MIME_TYPES).toContain('application/pdf');
        expect(ALLOWED_MIME_TYPES).toContain('image/gif');
        expect(ALLOWED_MIME_TYPES).toContain('image/png');
        expect(ALLOWED_MIME_TYPES).toContain('text/markdown');
    });
});
