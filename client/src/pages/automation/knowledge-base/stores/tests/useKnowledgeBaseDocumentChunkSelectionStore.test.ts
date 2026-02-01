import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {useKnowledgeBaseDocumentChunkSelectionStore} from '../useKnowledgeBaseDocumentChunkSelectionStore';

describe('useKnowledgeBaseDocumentChunkSelectionStore', () => {
    beforeEach(() => {
        const {result} = renderHook(() => useKnowledgeBaseDocumentChunkSelectionStore());

        act(() => {
            result.current.clearSelection();
        });
    });

    afterEach(() => {
        const {result} = renderHook(() => useKnowledgeBaseDocumentChunkSelectionStore());

        act(() => {
            result.current.clearSelection();
        });
    });

    describe('initial state', () => {
        it('has empty selectedChunks array', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkSelectionStore());

            expect(result.current.selectedChunks).toEqual([]);
        });
    });

    describe('toggleChunkSelection', () => {
        it('adds chunk to selection when not selected', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkSelectionStore());

            act(() => {
                result.current.toggleChunkSelection('chunk-1');
            });

            expect(result.current.selectedChunks).toContain('chunk-1');
        });

        it('removes chunk from selection when already selected', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkSelectionStore());

            act(() => {
                result.current.toggleChunkSelection('chunk-1');
            });

            act(() => {
                result.current.toggleChunkSelection('chunk-1');
            });

            expect(result.current.selectedChunks).not.toContain('chunk-1');
        });

        it('can select multiple chunks', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkSelectionStore());

            act(() => {
                result.current.toggleChunkSelection('chunk-1');
            });

            act(() => {
                result.current.toggleChunkSelection('chunk-2');
            });

            expect(result.current.selectedChunks).toEqual(['chunk-1', 'chunk-2']);
        });
    });

    describe('clearSelection', () => {
        it('clears all selected chunks', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkSelectionStore());

            act(() => {
                result.current.toggleChunkSelection('chunk-1');
                result.current.toggleChunkSelection('chunk-2');
            });

            act(() => {
                result.current.clearSelection();
            });

            expect(result.current.selectedChunks).toEqual([]);
        });
    });
});
