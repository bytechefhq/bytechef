import {act, renderHook} from '@testing-library/react';
import {ChangeEvent} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useUploadKnowledgeBaseDocumentDialog from '../useUploadKnowledgeBaseDocumentDialog';

const hoisted = vi.hoisted(() => {
    return {
        invalidateQueries: vi.fn(),
    };
});

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

vi.mock('@/shared/util/cookie-utils', () => ({
    getCookie: vi.fn(() => 'mock-xsrf-token'),
}));

describe('useUploadKnowledgeBaseDocumentDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('is closed', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.open).toBe(false);
        });

        it('has no selected files', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.selectedFiles).toEqual([]);
        });

        it('is not uploading', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.uploading).toBe(false);
        });

        it('cannot submit without files', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.canSubmit).toBe(false);
        });
    });

    describe('setOpen', () => {
        it('opens dialog', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.setOpen(true);
            });

            expect(result.current.open).toBe(true);
        });
    });

    describe('handleOpenChange', () => {
        it('opens dialog', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(result.current.open).toBe(true);
        });

        it('closes dialog and resets form', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            act(() => {
                result.current.setOpen(true);
            });

            const mockFile = new File(['content'], 'test.pdf', {type: 'application/pdf'});
            const mockEvent = {
                target: {files: [mockFile]},
            } as unknown as ChangeEvent<HTMLInputElement>;

            act(() => {
                result.current.handleFileChange(mockEvent);
            });

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(result.current.open).toBe(false);
            expect(result.current.selectedFiles).toEqual([]);
        });
    });

    describe('handleFileChange', () => {
        it('adds files to selected files', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            const mockFile = new File(['content'], 'test.pdf', {type: 'application/pdf'});
            const mockEvent = {
                target: {files: [mockFile]},
            } as unknown as ChangeEvent<HTMLInputElement>;

            act(() => {
                result.current.handleFileChange(mockEvent);
            });

            expect(result.current.selectedFiles).toHaveLength(1);
            expect(result.current.selectedFiles[0].file.name).toBe('test.pdf');
            expect(result.current.selectedFiles[0].status).toBe('pending');
        });

        it('can add multiple files', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            const mockFile1 = new File(['content1'], 'test1.pdf', {type: 'application/pdf'});
            const mockFile2 = new File(['content2'], 'test2.pdf', {type: 'application/pdf'});

            const mockEvent = {
                target: {files: [mockFile1, mockFile2]},
            } as unknown as ChangeEvent<HTMLInputElement>;

            act(() => {
                result.current.handleFileChange(mockEvent);
            });

            expect(result.current.selectedFiles).toHaveLength(2);
        });

        it('does nothing when no files selected', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            const mockEvent = {
                target: {files: null},
            } as unknown as ChangeEvent<HTMLInputElement>;

            act(() => {
                result.current.handleFileChange(mockEvent);
            });

            expect(result.current.selectedFiles).toEqual([]);
        });
    });

    describe('removeFile', () => {
        it('removes file at specified index', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            const mockFile1 = new File(['content1'], 'test1.pdf', {type: 'application/pdf'});
            const mockFile2 = new File(['content2'], 'test2.pdf', {type: 'application/pdf'});

            act(() => {
                result.current.handleFileChange({
                    target: {files: [mockFile1, mockFile2]},
                } as unknown as ChangeEvent<HTMLInputElement>);
            });

            act(() => {
                result.current.removeFile(0);
            });

            expect(result.current.selectedFiles).toHaveLength(1);
            expect(result.current.selectedFiles[0].file.name).toBe('test2.pdf');
        });
    });

    describe('canSubmit', () => {
        it('is true when files are selected and not uploading', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            const mockFile = new File(['content'], 'test.pdf', {type: 'application/pdf'});

            act(() => {
                result.current.handleFileChange({
                    target: {files: [mockFile]},
                } as unknown as ChangeEvent<HTMLInputElement>);
            });

            expect(result.current.canSubmit).toBe(true);
        });
    });

    describe('formatFileSize', () => {
        it('formats 0 bytes', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.formatFileSize(0)).toBe('0 Bytes');
        });

        it('formats bytes', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.formatFileSize(500)).toBe('500 Bytes');
        });

        it('formats kilobytes', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.formatFileSize(1024)).toBe('1 KB');
        });

        it('formats megabytes', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.formatFileSize(1048576)).toBe('1 MB');
        });

        it('formats gigabytes', () => {
            const {result} = renderHook(() => useUploadKnowledgeBaseDocumentDialog({knowledgeBaseId: 'kb-1'}));

            expect(result.current.formatFileSize(1073741824)).toBe('1 GB');
        });
    });
});
