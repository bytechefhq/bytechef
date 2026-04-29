import {
    KnowledgeBaseDocument,
    KnowledgeBaseDocumentChunk,
    useKnowledgeBaseDocumentTagsByDocumentQuery,
    useKnowledgeBaseDocumentTagsQuery,
} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

interface UseKnowledgeBaseDocumentListProps {
    documents: KnowledgeBaseDocument[];
}

export default function useKnowledgeBaseDocumentList({documents}: UseKnowledgeBaseDocumentListProps) {
    const {data: tagsByDocumentData} = useKnowledgeBaseDocumentTagsByDocumentQuery();
    const {data: allTagsData} = useKnowledgeBaseDocumentTagsQuery();

    const tagsByDocument = useMemo(
        () => tagsByDocumentData?.knowledgeBaseDocumentTagsByDocument ?? [],
        [tagsByDocumentData?.knowledgeBaseDocumentTagsByDocument]
    );

    const allTagNames = useMemo(
        () => allTagsData?.knowledgeBaseDocumentTags ?? [],
        [allTagsData?.knowledgeBaseDocumentTags]
    );

    const sortedChunksByDocument = useMemo(() => {
        const chunkMap = new Map<string, KnowledgeBaseDocumentChunk[]>();

        for (const document of documents) {
            const sortedChunks = (document.chunks || [])
                .filter((chunk): chunk is NonNullable<typeof chunk> => chunk !== null)
                .sort((chunkA, chunkB) => chunkA.id.localeCompare(chunkB.id));

            chunkMap.set(document.id, sortedChunks);
        }

        return chunkMap;
    }, [documents]);

    const getSortedChunksForDocument = (documentId: string): KnowledgeBaseDocumentChunk[] => {
        return sortedChunksByDocument.get(documentId) || [];
    };

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
        getSortedChunksForDocument,
        getTagsForDocument,
    };
}
