import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {KnowledgeBaseDocument} from '@/shared/middleware/graphql';

import KnowledgeBaseDocumentChunkList from './KnowledgeBaseDocumentChunkList';
import KnowledgeBaseDocumentListItem from './KnowledgeBaseDocumentListItem';
import KnowledgeBaseDocumentListItemDeleteDialog from './KnowledgeBaseDocumentListItemDeleteDialog';
import useKnowledgeBaseDocumentList from './hooks/useKnowledgeBaseDocumentList';

interface KnowledgeBaseDocumentListProps {
    documents: KnowledgeBaseDocument[];
    knowledgeBaseId: string;
}

const KnowledgeBaseDocumentList = ({documents, knowledgeBaseId}: KnowledgeBaseDocumentListProps) => {
    const {getRemainingTagsForDocument, getSortedChunksForDocument, getTagsForDocument} = useKnowledgeBaseDocumentList({
        documents,
    });

    return (
        <div className="w-full divide-y divide-border/50">
            {documents.length === 0 ? (
                <div className="rounded-lg border border-gray-200 bg-gray-50 p-8 text-center">
                    <p className="text-gray-500">No documents available. Upload documents to get started.</p>
                </div>
            ) : (
                <>
                    {documents.map((document) => {
                        const tags = getTagsForDocument(document.id);
                        const remainingTags = getRemainingTagsForDocument(document.id);
                        const sortedChunks = getSortedChunksForDocument(document.id);

                        return (
                            <Collapsible className="group" key={document.id}>
                                <KnowledgeBaseDocumentListItem
                                    document={document}
                                    remainingTags={remainingTags}
                                    tags={tags}
                                />

                                <CollapsibleContent>
                                    <KnowledgeBaseDocumentChunkList
                                        chunks={sortedChunks}
                                        documentName={document.name}
                                        knowledgeBaseId={knowledgeBaseId}
                                    />
                                </CollapsibleContent>
                            </Collapsible>
                        );
                    })}

                    <KnowledgeBaseDocumentListItemDeleteDialog knowledgeBaseId={knowledgeBaseId} />
                </>
            )}
        </div>
    );
};

export default KnowledgeBaseDocumentList;
