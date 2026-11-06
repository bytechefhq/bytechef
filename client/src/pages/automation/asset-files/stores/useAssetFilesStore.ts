import {create} from 'zustand';

interface AssetFilesStateI {
    searchQuery: string;
    selectedFileId: number | null;
    selectedTagIds: number[];
    setSearchQuery: (searchQuery: string) => void;
    setSelectedFileId: (selectedFileId: number | null) => void;
    setSelectedTagIds: (selectedTagIds: number[]) => void;
}

export const assetFilesStore = create<AssetFilesStateI>()((set) => ({
    searchQuery: '',
    selectedFileId: null,
    selectedTagIds: [],
    setSearchQuery: (searchQuery) => set({searchQuery}),
    setSelectedFileId: (selectedFileId) => set({selectedFileId}),
    setSelectedTagIds: (selectedTagIds) => set({selectedTagIds}),
}));

export const useAssetFilesStore = assetFilesStore;
