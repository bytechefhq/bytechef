import {act, renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import useKnowledgeBaseDropdownMenu from '../useKnowledgeBaseDropdownMenu';

describe('useKnowledgeBaseDropdownMenu', () => {
    describe('initial state', () => {
        it('has showDeleteDialog as false', () => {
            const {result} = renderHook(() => useKnowledgeBaseDropdownMenu());

            expect(result.current.showDeleteDialog).toBe(false);
        });

        it('has showEditDialog as false', () => {
            const {result} = renderHook(() => useKnowledgeBaseDropdownMenu());

            expect(result.current.showEditDialog).toBe(false);
        });
    });

    describe('handleShowDeleteDialog', () => {
        it('sets showDeleteDialog to true', () => {
            const {result} = renderHook(() => useKnowledgeBaseDropdownMenu());

            act(() => {
                result.current.handleShowDeleteDialog();
            });

            expect(result.current.showDeleteDialog).toBe(true);
        });
    });

    describe('handleCloseDeleteDialog', () => {
        it('sets showDeleteDialog to false', () => {
            const {result} = renderHook(() => useKnowledgeBaseDropdownMenu());

            act(() => {
                result.current.handleShowDeleteDialog();
            });

            act(() => {
                result.current.handleCloseDeleteDialog();
            });

            expect(result.current.showDeleteDialog).toBe(false);
        });
    });

    describe('handleShowEditDialog', () => {
        it('sets showEditDialog to true', () => {
            const {result} = renderHook(() => useKnowledgeBaseDropdownMenu());

            act(() => {
                result.current.handleShowEditDialog();
            });

            expect(result.current.showEditDialog).toBe(true);
        });
    });

    describe('handleCloseEditDialog', () => {
        it('sets showEditDialog to false', () => {
            const {result} = renderHook(() => useKnowledgeBaseDropdownMenu());

            act(() => {
                result.current.handleShowEditDialog();
            });

            act(() => {
                result.current.handleCloseEditDialog();
            });

            expect(result.current.showEditDialog).toBe(false);
        });
    });
});
