import {act, renderHook} from '@testing-library/react';
import {ChangeEvent} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useFileUploadDialog from '../useFileUploadDialog';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    uploadSelectedFile: vi.fn(),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

vi.mock('@/shared/util/file-upload-utils', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/util/file-upload-utils');

    return {
        ...actual,
        uploadSelectedFile: hoisted.uploadSelectedFile,
    };
});

const createFileChangeEvent = (files: File[]) => ({target: {files}}) as unknown as ChangeEvent<HTMLInputElement>;

const defaultProps = {
    fieldName: 'file',
    invalidateQueryKey: ['items'],
    url: '/api/upload',
};

describe('useFileUploadDialog', () => {
    beforeEach(() => {
        vi.useFakeTimers();

        hoisted.uploadSelectedFile.mockResolvedValue(undefined);
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.clearAllMocks();
    });

    it('adds selected files as pending', () => {
        const {result} = renderHook(() => useFileUploadDialog(defaultProps));

        act(() => {
            result.current.handleFileChange(createFileChangeEvent([new File(['a'], 'a.js')]));
        });

        expect(result.current.selectedFiles).toHaveLength(1);
        expect(result.current.selectedFiles[0].status).toBe('pending');
        expect(result.current.canSubmit).toBe(true);
    });

    it('drops files that acceptFile rejects', () => {
        const {result} = renderHook(() =>
            useFileUploadDialog({...defaultProps, acceptFile: (file) => file.name.endsWith('.js')})
        );

        act(() => {
            result.current.addFiles([new File(['a'], 'a.js'), new File(['b'], 'b.txt')]);
        });

        expect(result.current.selectedFiles.map((selectedFile) => selectedFile.file.name)).toEqual(['a.js']);
    });

    it('removes the file at the given index', () => {
        const {result} = renderHook(() => useFileUploadDialog(defaultProps));

        act(() => {
            result.current.addFiles([new File(['a'], 'a.js'), new File(['b'], 'b.js')]);
        });

        act(() => {
            result.current.removeFile(0);
        });

        expect(result.current.selectedFiles.map((selectedFile) => selectedFile.file.name)).toEqual(['b.js']);
    });

    it('uploads every file, invalidates the query and closes after a delay', async () => {
        const onReset = vi.fn();
        const {result} = renderHook(() => useFileUploadDialog({...defaultProps, onReset}));

        act(() => {
            result.current.setOpen(true);
            result.current.addFiles([new File(['a'], 'a.js'), new File(['b'], 'b.js')]);
        });

        await act(async () => {
            result.current.handleSubmit();
        });

        expect(hoisted.uploadSelectedFile).toHaveBeenCalledTimes(2);
        expect(hoisted.uploadSelectedFile).toHaveBeenCalledWith(
            expect.objectContaining({fieldName: 'file', index: 1, url: '/api/upload'})
        );
        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['items']});
        expect(result.current.uploading).toBe(true);
        expect(result.current.canSubmit).toBe(false);

        act(() => {
            vi.advanceTimersByTime(500);
        });

        expect(result.current.open).toBe(false);
        expect(result.current.selectedFiles).toEqual([]);
        expect(result.current.uploading).toBe(false);
        expect(onReset).toHaveBeenCalled();
    });

    it('does not upload when no file is selected', () => {
        const {result} = renderHook(() => useFileUploadDialog(defaultProps));

        act(() => {
            result.current.handleSubmit();
        });

        expect(hoisted.uploadSelectedFile).not.toHaveBeenCalled();
    });

    it('resets the selection when the dialog closes', () => {
        const onReset = vi.fn();
        const {result} = renderHook(() => useFileUploadDialog({...defaultProps, onReset}));

        act(() => {
            result.current.handleOpenChange(true);
            result.current.addFiles([new File(['a'], 'a.js')]);
        });

        act(() => {
            result.current.handleOpenChange(false);
        });

        expect(result.current.open).toBe(false);
        expect(result.current.selectedFiles).toEqual([]);
        expect(onReset).toHaveBeenCalledTimes(1);
    });
});
