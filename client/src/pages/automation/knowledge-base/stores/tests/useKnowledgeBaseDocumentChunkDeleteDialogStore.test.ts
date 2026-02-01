import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {useKnowledgeBaseDocumentChunkDeleteDialogStore} from '../useKnowledgeBaseDocumentChunkDeleteDialogStore';

describe('useKnowledgeBaseDocumentChunkDeleteDialogStore', () => {
    beforeEach(() => {
        const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialogStore());

        act(() => {
            result.current.clearDialog();
        });
    });

    afterEach(() => {
        const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialogStore());

        act(() => {
            result.current.clearDialog();
        });
    });

    describe('initial state', () => {
        it('has empty chunkIdsToDelete array', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialogStore());

            expect(result.current.chunkIdsToDelete).toEqual([]);
        });
    });

    describe('setChunkIdsToDelete', () => {
        it('sets chunk ids to delete', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialogStore());

            act(() => {
                result.current.setChunkIdsToDelete(['chunk-1', 'chunk-2']);
            });

            expect(result.current.chunkIdsToDelete).toEqual(['chunk-1', 'chunk-2']);
        });

        it('replaces existing chunk ids', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialogStore());

            act(() => {
                result.current.setChunkIdsToDelete(['chunk-1']);
            });

            act(() => {
                result.current.setChunkIdsToDelete(['chunk-2', 'chunk-3']);
            });

            expect(result.current.chunkIdsToDelete).toEqual(['chunk-2', 'chunk-3']);
        });
    });

    describe('clearDialog', () => {
        it('clears chunk ids to delete', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkDeleteDialogStore());

            act(() => {
                result.current.setChunkIdsToDelete(['chunk-1', 'chunk-2']);
            });

            act(() => {
                result.current.clearDialog();
            });

            expect(result.current.chunkIdsToDelete).toEqual([]);
        });
    });
});
