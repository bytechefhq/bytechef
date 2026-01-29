import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface KnowledgeBaseDocumentChunkSelectionStateI {
    clearSelection: () => void;
    selectedChunks: string[];
    toggleChunkSelection: (chunkId: string) => void;
}

export const useKnowledgeBaseDocumentChunkSelectionStore = create<KnowledgeBaseDocumentChunkSelectionStateI>()(
    devtools(
        (set) => ({
            clearSelection: () => {
                set(() => ({
                    selectedChunks: [],
                }));
            },

            selectedChunks: [],

            toggleChunkSelection: (chunkId: string) => {
                set((state) => ({
                    selectedChunks: state.selectedChunks.includes(chunkId)
                        ? state.selectedChunks.filter((id) => id !== chunkId)
                        : [...state.selectedChunks, chunkId],
                }));
            },
        }),
        {
            name: 'bytechef.knowledge-base-document-chunk-selection',
        }
    )
);
