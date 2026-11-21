import {Dispatch, SetStateAction} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {UploadFileI, formatFileSize, uploadSelectedFile} from '../file-upload-utils';

vi.mock('@/shared/util/cookie-utils', () => ({
    getCookie: vi.fn(() => 'mock-xsrf-token'),
}));

const createFileState = (initialFiles: UploadFileI[]) => {
    let currentFiles = initialFiles;

    const setSelectedFiles: Dispatch<SetStateAction<UploadFileI[]>> = (action) => {
        currentFiles = typeof action === 'function' ? action(currentFiles) : action;
    };

    return {getFiles: () => currentFiles, setSelectedFiles};
};

describe('formatFileSize', () => {
    it('formats 0 bytes', () => {
        expect(formatFileSize(0)).toBe('0 Bytes');
    });

    it('formats bytes, kilobytes, megabytes and gigabytes', () => {
        expect(formatFileSize(500)).toBe('500 Bytes');
        expect(formatFileSize(1536)).toBe('1.5 KB');
        expect(formatFileSize(1048576)).toBe('1 MB');
        expect(formatFileSize(1073741824)).toBe('1 GB');
    });
});

describe('uploadSelectedFile', () => {
    const fetchMock = vi.fn();

    beforeEach(() => {
        vi.stubGlobal('fetch', fetchMock);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
    });

    it('posts the file under the field name with the XSRF token and marks it completed', async () => {
        const file = new File(['content'], 'component.js');
        const fileState = createFileState([
            {file: new File(['other'], 'other.js'), status: 'pending'},
            {file, status: 'pending'},
        ]);

        fetchMock.mockResolvedValue({ok: true});

        await uploadSelectedFile({
            fieldName: 'componentFile',
            file,
            index: 1,
            setSelectedFiles: fileState.setSelectedFiles,
            url: '/api/upload',
        });

        const [url, requestInit] = fetchMock.mock.calls[0];

        expect(url).toBe('/api/upload');
        expect(requestInit.method).toBe('POST');
        expect(requestInit.headers).toEqual({'X-XSRF-TOKEN': 'mock-xsrf-token'});
        expect((requestInit.body as FormData).get('componentFile')).toBeInstanceOf(File);
        expect(fileState.getFiles()[0].status).toBe('pending');
        expect(fileState.getFiles()[1]).toEqual({file, status: 'completed', statusMessage: 'Uploaded successfully'});
    });

    it('marks the file uploading while the request is in flight', async () => {
        const file = new File(['content'], 'component.js');
        const fileState = createFileState([{file, status: 'pending'}]);

        let resolveResponse: (response: {ok: boolean}) => void = () => undefined;

        fetchMock.mockReturnValue(
            new Promise((resolve) => {
                resolveResponse = resolve;
            })
        );

        const uploadPromise = uploadSelectedFile({
            fieldName: 'file',
            file,
            index: 0,
            setSelectedFiles: fileState.setSelectedFiles,
            url: '/api/upload',
        });

        expect(fileState.getFiles()[0].status).toBe('uploading');

        resolveResponse({ok: true});

        await uploadPromise;

        expect(fileState.getFiles()[0].status).toBe('completed');
    });

    it('marks the file failed with the response status text when the response is not ok', async () => {
        const file = new File(['content'], 'component.js');
        const fileState = createFileState([{file, status: 'pending'}]);

        fetchMock.mockResolvedValue({ok: false, statusText: 'Bad Request'});

        await uploadSelectedFile({
            fieldName: 'file',
            file,
            index: 0,
            setSelectedFiles: fileState.setSelectedFiles,
            url: '/api/upload',
        });

        expect(fileState.getFiles()[0]).toEqual({file, status: 'error', statusMessage: 'Upload failed: Bad Request'});
    });

    it('marks the file failed with a generic message when the request rejects with a non error', async () => {
        const file = new File(['content'], 'component.js');
        const fileState = createFileState([{file, status: 'pending'}]);

        fetchMock.mockRejectedValue('network down');

        await uploadSelectedFile({
            fieldName: 'file',
            file,
            index: 0,
            setSelectedFiles: fileState.setSelectedFiles,
            url: '/api/upload',
        });

        expect(fileState.getFiles()[0]).toEqual({file, status: 'error', statusMessage: 'Upload failed'});
    });
});
