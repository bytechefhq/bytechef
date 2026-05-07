import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, test} from 'vitest';

import {assetFilesStore, useAssetFilesStore} from './useAssetFilesStore';

describe('useAssetFilesStore', () => {
    beforeEach(() => {
        assetFilesStore.setState({
            searchQuery: '',
            selectedFileId: null,
            selectedTagIds: [],
        });
    });

    test('sets search query', () => {
        const {result} = renderHook(() => useAssetFilesStore());

        act(() => result.current.setSearchQuery('spec'));

        expect(assetFilesStore.getState().searchQuery).toBe('spec');
    });

    test('sets selected tag ids', () => {
        const {result} = renderHook(() => useAssetFilesStore());

        act(() => result.current.setSelectedTagIds([1, 2, 3]));

        expect(assetFilesStore.getState().selectedTagIds).toEqual([1, 2, 3]);
    });

    test('sets selected file id', () => {
        const {result} = renderHook(() => useAssetFilesStore());

        act(() => result.current.setSelectedFileId(42));

        expect(assetFilesStore.getState().selectedFileId).toBe(42);
    });
});
