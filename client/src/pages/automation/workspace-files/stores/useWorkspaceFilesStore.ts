import {create} from 'zustand';

interface WorkspaceFilesStateI {
    searchQuery: string;
    selectedFileId: number | null;
    selectedTagIds: number[];
    setSearchQuery: (searchQuery: string) => void;
    setSelectedFileId: (selectedFileId: number | null) => void;
    setSelectedTagIds: (selectedTagIds: number[]) => void;
}

export const workspaceFilesStore = create<WorkspaceFilesStateI>()((set) => ({
    searchQuery: '',
    selectedFileId: null,
    selectedTagIds: [],
    setSearchQuery: (searchQuery) => set({searchQuery}),
    setSelectedFileId: (selectedFileId) => set({selectedFileId}),
    setSelectedTagIds: (selectedTagIds) => set({selectedTagIds}),
}));

export const useWorkspaceFilesStore = workspaceFilesStore;
