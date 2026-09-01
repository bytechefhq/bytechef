import {
    useKnowledgeBaseDocumentTagsByDocumentQuery,
    useKnowledgeBaseDocumentTagsQuery,
} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

export default function useKnowledgeBaseDocumentList({knowledgeBaseId}: {knowledgeBaseId: string}) {
    const {data: tagsByDocumentData} = useKnowledgeBaseDocumentTagsByDocumentQuery({knowledgeBaseId});
    const {data: allTagsData} = useKnowledgeBaseDocumentTagsQuery({knowledgeBaseId});

    const tagsByDocument = useMemo(
        () => tagsByDocumentData?.knowledgeBaseDocumentTagsByDocument ?? [],
        [tagsByDocumentData?.knowledgeBaseDocumentTagsByDocument]
    );

    const allTagNames = useMemo(
        () => allTagsData?.knowledgeBaseDocumentTags ?? [],
        [allTagsData?.knowledgeBaseDocumentTags]
    );

    const getTagsForDocument = (documentId: string): string[] => {
        const entry = tagsByDocument.find((tagEntry) => tagEntry.knowledgeBaseDocumentId === documentId);

        return (entry?.tags as string[]) ?? [];
    };

    const getRemainingTagsForDocument = (documentId: string): string[] => {
        const documentTagNames = new Set(getTagsForDocument(documentId));

        return allTagNames.filter((tagName) => !documentTagNames.has(tagName));
    };

    return {
        getRemainingTagsForDocument,
        getTagsForDocument,
    };
}
