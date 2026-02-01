import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {useKnowledgeBaseDocumentDeleteDialogStore} from '../useKnowledgeBaseDocumentDeleteDialogStore';

describe('useKnowledgeBaseDocumentDeleteDialogStore', () => {
    beforeEach(() => {
        const {result} = renderHook(() => useKnowledgeBaseDocumentDeleteDialogStore());

        act(() => {
            result.current.clearDialog();
        });
    });

    afterEach(() => {
        const {result} = renderHook(() => useKnowledgeBaseDocumentDeleteDialogStore());

        act(() => {
            result.current.clearDialog();
        });
    });

    describe('initial state', () => {
        it('has null documentId', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentDeleteDialogStore());

            expect(result.current.documentId).toBeNull();
        });
    });

    describe('setDocumentId', () => {
        it('sets document id', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentDeleteDialogStore());

            act(() => {
                result.current.setDocumentId('doc-1');
            });

            expect(result.current.documentId).toBe('doc-1');
        });

        it('replaces existing document id', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentDeleteDialogStore());

            act(() => {
                result.current.setDocumentId('doc-1');
            });

            act(() => {
                result.current.setDocumentId('doc-2');
            });

            expect(result.current.documentId).toBe('doc-2');
        });
    });

    describe('clearDialog', () => {
        it('clears document id', () => {
            const {result} = renderHook(() => useKnowledgeBaseDocumentDeleteDialogStore());

            act(() => {
                result.current.setDocumentId('doc-1');
            });

            act(() => {
                result.current.clearDialog();
            });

            expect(result.current.documentId).toBeNull();
        });
    });
});
