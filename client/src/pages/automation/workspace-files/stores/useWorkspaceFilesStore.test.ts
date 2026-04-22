import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, test} from 'vitest';

import {useWorkspaceFilesStore, workspaceFilesStore} from './useWorkspaceFilesStore';

describe('useWorkspaceFilesStore', () => {
    beforeEach(() => {
        workspaceFilesStore.setState({
            searchQuery: '',
            selectedFileId: null,
            selectedTagIds: [],
        });
    });

    test('sets search query', () => {
        const {result} = renderHook(() => useWorkspaceFilesStore());

        act(() => result.current.setSearchQuery('spec'));

        expect(workspaceFilesStore.getState().searchQuery).toBe('spec');
    });

    test('sets selected tag ids', () => {
        const {result} = renderHook(() => useWorkspaceFilesStore());

        act(() => result.current.setSelectedTagIds([1, 2, 3]));

        expect(workspaceFilesStore.getState().selectedTagIds).toEqual([1, 2, 3]);
    });

    test('sets selected file id', () => {
        const {result} = renderHook(() => useWorkspaceFilesStore());

        act(() => result.current.setSelectedFileId(42));

        expect(workspaceFilesStore.getState().selectedFileId).toBe(42);
    });
});
