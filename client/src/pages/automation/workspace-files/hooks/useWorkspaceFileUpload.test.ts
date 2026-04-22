import {renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, test, vi} from 'vitest';

import {useWorkspaceFileUpload} from './useWorkspaceFileUpload';

describe('useWorkspaceFileUpload', () => {
    beforeEach(() => {
        global.fetch = vi.fn().mockResolvedValue({
            json: async () => ({id: 1, mimeType: 'text/markdown', name: 'spec.md', sizeBytes: 5}),
            ok: true,
            status: 201,
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    test('upload posts multipart with workspaceId and file', async () => {
        const {result} = renderHook(() => useWorkspaceFileUpload());
        const file = new File(['# hi'], 'spec.md', {type: 'text/markdown'});

        const response = await result.current.upload(42, file);

        expect(response.id).toBe(1);
        expect(global.fetch).toHaveBeenCalledWith(
            '/api/automation/internal/workspace-files/upload',
            expect.objectContaining({body: expect.any(FormData), method: 'POST'})
        );
    });

    test('throws on non-ok response', async () => {
        global.fetch = vi.fn().mockResolvedValue({
            ok: false,
            status: 413,
            statusText: 'Payload Too Large',
        });
        const {result} = renderHook(() => useWorkspaceFileUpload());
        const file = new File(['big'], 'big.bin');

        await expect(result.current.upload(42, file)).rejects.toThrow();
    });
});
