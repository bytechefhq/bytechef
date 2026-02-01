import {act, renderHook} from '@testing-library/react';
import {MouseEvent} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useKnowledgeBaseListItem from '../useKnowledgeBaseListItem';

const hoisted = vi.hoisted(() => {
    return {
        navigate: vi.fn(),
    };
});

vi.mock('react-router-dom', () => ({
    useNavigate: vi.fn(() => hoisted.navigate),
}));

const mockKnowledgeBase = {
    id: 'kb-1',
    name: 'Test KB',
};

describe('useKnowledgeBaseListItem', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('has showDeleteDialog as false', () => {
            const {result} = renderHook(() => useKnowledgeBaseListItem({knowledgeBase: mockKnowledgeBase}));

            expect(result.current.showDeleteDialog).toBe(false);
        });

        it('has showEditDialog as false', () => {
            const {result} = renderHook(() => useKnowledgeBaseListItem({knowledgeBase: mockKnowledgeBase}));

            expect(result.current.showEditDialog).toBe(false);
        });
    });

    describe('handleShowDeleteDialog', () => {
        it('sets showDeleteDialog to true', () => {
            const {result} = renderHook(() => useKnowledgeBaseListItem({knowledgeBase: mockKnowledgeBase}));

            act(() => {
                result.current.handleShowDeleteDialog();
            });

            expect(result.current.showDeleteDialog).toBe(true);
        });
    });

    describe('handleCloseDeleteDialog', () => {
        it('sets showDeleteDialog to false', () => {
            const {result} = renderHook(() => useKnowledgeBaseListItem({knowledgeBase: mockKnowledgeBase}));

            act(() => {
                result.current.handleShowDeleteDialog();
            });

            act(() => {
                result.current.handleCloseDeleteDialog();
            });

            expect(result.current.showDeleteDialog).toBe(false);
        });
    });

    describe('handleEditClick', () => {
        it('sets showEditDialog to true', () => {
            const {result} = renderHook(() => useKnowledgeBaseListItem({knowledgeBase: mockKnowledgeBase}));

            const mockEvent = {
                stopPropagation: vi.fn(),
            } as unknown as MouseEvent;

            act(() => {
                result.current.handleEditClick(mockEvent);
            });

            expect(result.current.showEditDialog).toBe(true);
        });

        it('stops event propagation', () => {
            const {result} = renderHook(() => useKnowledgeBaseListItem({knowledgeBase: mockKnowledgeBase}));

            const mockEvent = {
                stopPropagation: vi.fn(),
            } as unknown as MouseEvent;

            act(() => {
                result.current.handleEditClick(mockEvent);
            });

            expect(mockEvent.stopPropagation).toHaveBeenCalled();
        });
    });

    describe('handleEditDialogOpenChange', () => {
        it('updates showEditDialog state', () => {
            const {result} = renderHook(() => useKnowledgeBaseListItem({knowledgeBase: mockKnowledgeBase}));

            act(() => {
                result.current.handleEditDialogOpenChange(true);
            });

            expect(result.current.showEditDialog).toBe(true);

            act(() => {
                result.current.handleEditDialogOpenChange(false);
            });

            expect(result.current.showEditDialog).toBe(false);
        });
    });

    describe('handleKnowledgeBaseClick', () => {
        it('navigates to knowledge base detail page', () => {
            const {result} = renderHook(() => useKnowledgeBaseListItem({knowledgeBase: mockKnowledgeBase}));

            act(() => {
                result.current.handleKnowledgeBaseClick();
            });

            expect(hoisted.navigate).toHaveBeenCalledWith('/automation/knowledge-bases/kb-1');
        });
    });

    describe('handleTagListClick', () => {
        it('prevents default and stops propagation', () => {
            const {result} = renderHook(() => useKnowledgeBaseListItem({knowledgeBase: mockKnowledgeBase}));

            const mockEvent = {
                preventDefault: vi.fn(),
                stopPropagation: vi.fn(),
            } as unknown as MouseEvent;

            act(() => {
                result.current.handleTagListClick(mockEvent);
            });

            expect(mockEvent.preventDefault).toHaveBeenCalled();
            expect(mockEvent.stopPropagation).toHaveBeenCalled();
        });
    });
});
