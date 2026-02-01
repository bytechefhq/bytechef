import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useEditKnowledgeBaseDialog from '../useEditKnowledgeBaseDialog';

const hoisted = vi.hoisted(() => {
    return {
        invalidateQueries: vi.fn(),
        mutate: vi.fn(),
        toast: vi.fn(),
    };
});

vi.mock('@/hooks/use-toast', () => ({
    useToast: vi.fn(() => ({
        toast: hoisted.toast,
    })),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: hoisted.invalidateQueries,
    })),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useUpdateKnowledgeBaseMutation: vi.fn(() => ({
        isPending: false,
        mutate: hoisted.mutate,
    })),
}));

const mockKnowledgeBase = {
    description: 'Test description',
    id: 'kb-1',
    maxChunkSize: 1024,
    minChunkSizeChars: 1,
    name: 'Test KB',
    overlap: 200,
};

describe('useEditKnowledgeBaseDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('initializes with knowledge base name', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            expect(result.current.name).toBe('Test KB');
        });

        it('initializes with knowledge base description', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            expect(result.current.description).toBe('Test description');
        });

        it('initializes with empty description when null', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: {...mockKnowledgeBase, description: null},
                })
            );

            expect(result.current.description).toBe('');
        });

        it('is closed by default in uncontrolled mode', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            expect(result.current.open).toBe(false);
        });
    });

    describe('controlled mode', () => {
        it('uses controlled open state', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                    open: true,
                })
            );

            expect(result.current.open).toBe(true);
        });
    });

    describe('handleNameChange', () => {
        it('updates name', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            act(() => {
                result.current.handleNameChange('New Name');
            });

            expect(result.current.name).toBe('New Name');
        });
    });

    describe('handleDescriptionChange', () => {
        it('updates description', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            act(() => {
                result.current.handleDescriptionChange('New Description');
            });

            expect(result.current.description).toBe('New Description');
        });
    });

    describe('handleOpenChange', () => {
        it('updates internal open state in uncontrolled mode', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            act(() => {
                result.current.handleOpenChange(true);
            });

            expect(result.current.open).toBe(true);
        });

        it('calls onOpenChange in controlled mode', () => {
            const mockOnOpenChange = vi.fn();
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                    onOpenChange: mockOnOpenChange,
                    open: true,
                })
            );

            act(() => {
                result.current.handleOpenChange(false);
            });

            expect(mockOnOpenChange).toHaveBeenCalledWith(false);
        });
    });

    describe('handleCancel', () => {
        it('closes dialog', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            act(() => {
                result.current.handleOpenChange(true);
            });

            act(() => {
                result.current.handleCancel();
            });

            expect(result.current.open).toBe(false);
        });
    });

    describe('handleSave', () => {
        it('calls mutation with updated values', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            act(() => {
                result.current.handleNameChange('Updated Name');
                result.current.handleDescriptionChange('Updated Description');
            });

            act(() => {
                result.current.handleSave();
            });

            expect(hoisted.mutate).toHaveBeenCalledWith({
                id: 'kb-1',
                knowledgeBase: {
                    description: 'Updated Description',
                    maxChunkSize: 1024,
                    minChunkSizeChars: 1,
                    name: 'Updated Name',
                    overlap: 200,
                },
            });
        });

        it('trims whitespace from name', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            act(() => {
                result.current.handleNameChange('  Trimmed Name  ');
            });

            act(() => {
                result.current.handleSave();
            });

            expect(hoisted.mutate).toHaveBeenCalledWith(
                expect.objectContaining({
                    knowledgeBase: expect.objectContaining({
                        name: 'Trimmed Name',
                    }),
                })
            );
        });

        it('sets description to undefined when empty', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            act(() => {
                result.current.handleDescriptionChange('   ');
            });

            act(() => {
                result.current.handleSave();
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

    describe('canSubmit', () => {
        it('is true when name is not empty', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            expect(result.current.canSubmit).toBe(true);
        });

        it('is false when name is empty', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            act(() => {
                result.current.handleNameChange('');
            });

            expect(result.current.canSubmit).toBe(false);
        });

        it('is false when name is only whitespace', () => {
            const {result} = renderHook(() =>
                useEditKnowledgeBaseDialog({
                    knowledgeBase: mockKnowledgeBase,
                })
            );

            act(() => {
                result.current.handleNameChange('   ');
            });

            expect(result.current.canSubmit).toBe(false);
        });
    });
});
