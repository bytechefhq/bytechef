import {KnowledgeBaseDocumentChunk} from '@/shared/middleware/graphql';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface KnowledgeBaseDocumentChunkEditDialogStateI {
    chunk: KnowledgeBaseDocumentChunk | null;
    clearDialog: () => void;
    content: string;
    setChunk: (chunk: KnowledgeBaseDocumentChunk) => void;
    setContent: (content: string) => void;
}

export const useKnowledgeBaseDocumentChunkEditDialogStore = create<KnowledgeBaseDocumentChunkEditDialogStateI>()(
    devtools(
        (set) => ({
            chunk: null,

            clearDialog: () => {
                set(() => ({
                    chunk: null,
                    content: '',
                }));
            },

            content: '',

            setChunk: (chunk: KnowledgeBaseDocumentChunk) => {
                set(() => ({
                    chunk,
                    content: chunk.content ?? '',
                }));
            },

            setContent: (content: string) => {
                set(() => ({content}));
            },
        }),
        {
            name: 'bytechef.knowledge-base-document-chunk-edit-dialog',
        }
    )
);
