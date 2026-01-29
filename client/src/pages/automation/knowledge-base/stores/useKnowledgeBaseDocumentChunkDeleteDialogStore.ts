import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface KnowledgeBaseDocumentChunkDeleteDialogStateI {
    chunkIdsToDelete: string[];
    clearDialog: () => void;
    setChunkIdsToDelete: (chunkIds: string[]) => void;
}

export const useKnowledgeBaseDocumentChunkDeleteDialogStore = create<KnowledgeBaseDocumentChunkDeleteDialogStateI>()(
    devtools(
        (set) => ({
            chunkIdsToDelete: [],

            clearDialog: () => {
                set(() => ({
                    chunkIdsToDelete: [],
                }));
            },

            setChunkIdsToDelete: (chunkIds: string[]) => {
                set(() => ({
                    chunkIdsToDelete: chunkIds,
                }));
            },
        }),
        {
            name: 'bytechef.knowledge-base-document-chunk-delete-dialog',
        }
    )
);
