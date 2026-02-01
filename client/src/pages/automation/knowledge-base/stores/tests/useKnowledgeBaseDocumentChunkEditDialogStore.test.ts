import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {useKnowledgeBaseDocumentChunkEditDialogStore} from '../useKnowledgeBaseDocumentChunkEditDialogStore';

const mockChunk = {
    content: 'Test content',
    id: 'chunk-1',
    knowledgeBaseDocumentId: 'doc-1',
    metadata: null,
};

describe('useKnowledgeBaseDocumentChunkEditDialogStore', () => {
    beforeEach(() => {
        const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialogStore());

        act(() => {
            result.current.clearDialog();
        });
    });

    afterEach(() => {
        const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialogStore());

        act(() => {
            result.current.clearDialog();
        });
    });

    describe('initial state', () => {
        it('has null chunk', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialogStore());

            expect(result.current.chunk).toBeNull();
        });

        it('has empty content', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialogStore());

            expect(result.current.content).toBe('');
        });
    });

    describe('setChunk', () => {
        it('sets chunk and content from chunk', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialogStore());

            act(() => {
                result.current.setChunk(mockChunk);
            });

            expect(result.current.chunk).toEqual(mockChunk);
            expect(result.current.content).toBe('Test content');
        });

        it('sets empty content when chunk content is null', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialogStore());

            const chunkWithNullContent = {...mockChunk, content: null};

            act(() => {
                result.current.setChunk(chunkWithNullContent);
            });

            expect(result.current.content).toBe('');
        });
    });

    describe('setContent', () => {
        it('updates content', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialogStore());

            act(() => {
                result.current.setContent('New content');
            });

            expect(result.current.content).toBe('New content');
        });
    });

    describe('clearDialog', () => {
        it('resets chunk and content', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentChunkEditDialogStore());

            act(() => {
                result.current.setChunk(mockChunk);
            });

            act(() => {
                result.current.clearDialog();
            });

            expect(result.current.chunk).toBeNull();
            expect(result.current.content).toBe('');
        });
    });
});
