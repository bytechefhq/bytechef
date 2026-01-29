import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface KnowledgeBaseDocumentDeleteDialogStateI {
    clearDialog: () => void;
    documentId: string | null;
    setDocumentId: (documentId: string) => void;
}

export const useKnowledgeBaseDocumentDeleteDialogStore = create<KnowledgeBaseDocumentDeleteDialogStateI>()(
    devtools(
        (set) => ({
            clearDialog: () => {
                set(() => ({
                    documentId: null,
                }));
            },

            documentId: null,

            setDocumentId: (documentId: string) => {
                set(() => ({
                    documentId,
                }));
            },
        }),
        {
            name: 'bytechef.knowledge-base-document-delete-dialog',
        }
    )
);
