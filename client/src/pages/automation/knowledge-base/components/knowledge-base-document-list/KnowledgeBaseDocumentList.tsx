import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {
    KnowledgeBaseDocument,
    Tag,
    useKnowledgeBaseDocumentTagsByDocumentQuery,
    useKnowledgeBaseDocumentTagsQuery,
} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

import KnowledgeBaseDocumentChunkList from './KnowledgeBaseDocumentChunkList';
import KnowledgeBaseDocumentListItem from './KnowledgeBaseDocumentListItem';

interface KnowledgeBaseDocumentListProps {
    documents: KnowledgeBaseDocument[];
    knowledgeBaseId: string;
}

const KnowledgeBaseDocumentList = ({documents, knowledgeBaseId}: KnowledgeBaseDocumentListProps) => {
    const {data: tagsByDocumentData} = useKnowledgeBaseDocumentTagsByDocumentQuery();
    const {data: allTagsData} = useKnowledgeBaseDocumentTagsQuery();

    const tagsByDocument = useMemo(
        () => tagsByDocumentData?.knowledgeBaseDocumentTagsByDocument ?? [],
        [tagsByDocumentData?.knowledgeBaseDocumentTagsByDocument]
    );

    const allTags = useMemo(
        () => allTagsData?.knowledgeBaseDocumentTags ?? [],
        [allTagsData?.knowledgeBaseDocumentTags]
    );

    const getTagsForDocument = (documentId: string): Tag[] => {
        const entry = tagsByDocument.find((tagEntry) => tagEntry.knowledgeBaseDocumentId === documentId);

        return (entry?.tags as Tag[]) ?? [];
    };

    const getRemainingTagsForDocument = (documentId: string): Tag[] => {
        const documentTags = getTagsForDocument(documentId);
        const documentTagIds = new Set(documentTags.map((tag) => tag.id));

        return allTags.filter((tag) => !documentTagIds.has(tag.id));
    };

    return (
        <div className="w-full divide-y divide-border/50">
            {documents.length === 0 ? (
                <div className="rounded-lg border border-gray-200 bg-gray-50 p-8 text-center">
                    <p className="text-gray-500">No documents available. Upload documents to get started.</p>
                </div>
            ) : (
                documents.map((document) => (
                    <Collapsible className="group" key={document.id}>
                        <KnowledgeBaseDocumentListItem
                            document={document}
                            knowledgeBaseId={knowledgeBaseId}
                            remainingTags={getRemainingTagsForDocument(document.id)}
                            tags={getTagsForDocument(document.id)}
                        />

                        <CollapsibleContent>
                            <KnowledgeBaseDocumentChunkList
                                chunks={(document.chunks || [])
                                    .filter((chunk): chunk is NonNullable<typeof chunk> => chunk !== null)
                                    .sort((a, b) => a.id.localeCompare(b.id))}
                                documentName={document.name}
                                knowledgeBaseId={knowledgeBaseId}
                            />
                        </CollapsibleContent>
                    </Collapsible>
                ))
            )}
        </div>
    );
};

export default KnowledgeBaseDocumentList;
