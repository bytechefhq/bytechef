import {act, renderHook} from '@testing-library/react';
import {ChangeEvent} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useCreateKnowledgeBaseDialog from '../useCreateKnowledgeBaseDialog';

const hoisted = vi.hoisted(() => {
    return {
        invalidateQueries: vi.fn(),
        mutate: vi.fn(),
    };
});

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateKnowledgeBaseMutation: vi.fn(() => ({
        isPending: false,
        mutate: hoisted.mutate,
    })),
}));

describe('useCreateKnowledgeBaseDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('is closed', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.open).toBe(false);
        });

        it('has empty name', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.name).toBe('');
        });

        it('has empty description', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.description).toBe('');
        });

        it('has default minChunkSizeChars', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.minChunkSizeChars).toBe('1');
        });

        it('has default maxChunkSize', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.maxChunkSize).toBe('1024');
        });

        it('has default overlapSize', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.overlapSize).toBe('200');
        });

        it('has no selected files', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.selectedFiles).toEqual([]);
        });

        it('is not uploading', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.uploading).toBe(false);
        });
    });

    describe('setName', () => {
        it('updates name', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setName('New KB');
            });

            expect(result.current.name).toBe('New KB');
        });
    });

    describe('setDescription', () => {
        it('updates description', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setDescription('New description');
            });

            expect(result.current.description).toBe('New description');
        });
    });

    describe('setMinChunkSizeChars', () => {
        it('updates minChunkSizeChars', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setMinChunkSizeChars('5');
            });

            expect(result.current.minChunkSizeChars).toBe('5');
        });
    });

    describe('setMaxChunkSize', () => {
        it('updates maxChunkSize', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setMaxChunkSize('2048');
            });

            expect(result.current.maxChunkSize).toBe('2048');
        });
    });

    describe('setOverlapSize', () => {
        it('updates overlapSize', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setOverlapSize('100');
            });

            expect(result.current.overlapSize).toBe('100');
        });
    });

    describe('handleFileChange', () => {
        it('adds files to selected files', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            const mockFile = new File(['content'], 'test.pdf', {type: 'application/pdf'});

            act(() => {
                result.current.handleFileChange({
                    target: {files: [mockFile]},
                } as unknown as ChangeEvent<HTMLInputElement>);
            });

            expect(result.current.selectedFiles).toHaveLength(1);
            expect(result.current.selectedFiles[0].file.name).toBe('test.pdf');
            expect(result.current.selectedFiles[0].status).toBe('pending');
        });
    });

    describe('removeFile', () => {
        it('removes file at index', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

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
        it('is false when name is empty', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.canSubmit).toBe(false);
        });

        it('is true when name is not empty', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setName('Test KB');
            });

            expect(result.current.canSubmit).toBe(true);
        });

        it('is false when name is only whitespace', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setName('   ');
            });

            expect(result.current.canSubmit).toBe(false);
        });
    });

    describe('handleSubmit', () => {
        it('calls mutation with form data', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setName('Test KB');
                result.current.setDescription('Test description');
                result.current.setMinChunkSizeChars('5');
                result.current.setMaxChunkSize('2048');
                result.current.setOverlapSize('100');
            });

            act(() => {
                result.current.handleSubmit();
            });

            expect(hoisted.mutate).toHaveBeenCalledWith({
                knowledgeBase: {
                    description: 'Test description',
                    maxChunkSize: 2048,
                    minChunkSizeChars: 5,
                    name: 'Test KB',
                    overlap: 100,
                },
                workspaceId: 'ws-1',
            });
        });

        it('trims whitespace from name', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setName('  Test KB  ');
            });

            act(() => {
                result.current.handleSubmit();
            });

            expect(hoisted.mutate).toHaveBeenCalledWith(
                expect.objectContaining({
                    knowledgeBase: expect.objectContaining({
                        name: 'Test KB',
                    }),
                })
            );
        });

        it('sets description to undefined when empty', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setName('Test KB');
                result.current.setDescription('');
            });

            act(() => {
                result.current.handleSubmit();
            });

            expect(hoisted.mutate).toHaveBeenCalledWith(
                expect.objectContaining({
                    knowledgeBase: expect.objectContaining({
                        description: undefined,
                    }),
                })
            );
        });
    });

    describe('handleOpenChange', () => {
        it('opens dialog', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(result.current.open).toBe(true);
        });

        it('closes dialog and resets form', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            act(() => {
                result.current.setOpen(true);
                result.current.setName('Test');
            });

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(result.current.open).toBe(false);
            expect(result.current.name).toBe('');
        });
    });

    describe('formatFileSize', () => {
        it('formats 0 bytes', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.formatFileSize(0)).toBe('0 Bytes');
        });

        it('formats kilobytes', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.formatFileSize(1024)).toBe('1 KB');
        });

        it('formats megabytes', () => {
            const {result} = renderHook(() => useCreateKnowledgeBaseDialog({workspaceId: 'ws-1'}));

            expect(result.current.formatFileSize(1048576)).toBe('1 MB');
        });
    });
});
