import KnowledgeBaseDocumentChunkDeleteDialog from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkDeleteDialog';
import KnowledgeBaseDocumentChunkEditDialog from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkEditDialog';
import KnowledgeBaseDocumentChunkListItemDropdownMenu from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkListItemDropdownMenu';
import KnowledgeBaseDocumentChunkListItemHeader from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkListItemHeader';
import KnowledgeBaseDocumentChunkListSelectionBar from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkListSelectionBar';
import {KnowledgeBaseDocumentChunk} from '@/shared/middleware/graphql';

interface KnowledgeBaseDocumentChunkListProps {
    chunks: KnowledgeBaseDocumentChunk[];
    documentName: string;
    knowledgeBaseId: string;
}

const KnowledgeBaseDocumentChunkList = ({
    chunks,
    documentName,
    knowledgeBaseId,
}: KnowledgeBaseDocumentChunkListProps) => {
    if (chunks.length === 0) {
        return (
            <div className="ml-7 border-l border-border/50 py-2 pl-4">
                <p className="text-sm text-muted-foreground">No chunks available for this document.</p>
            </div>
        );
    }

    return (
        <div className="ml-7 space-y-2 border-l border-border/50 py-2 pl-4">
            <KnowledgeBaseDocumentChunkListSelectionBar />

            {chunks.map((chunk, index) => (
                <div className="rounded-lg border border-gray-200 bg-white p-4" key={chunk.id}>
                    <div className="mb-2 flex items-center justify-between">
                        <KnowledgeBaseDocumentChunkListItemHeader
                            chunkId={chunk.id}
                            chunkIndex={index}
                            documentName={documentName}
                        />

                        <KnowledgeBaseDocumentChunkListItemDropdownMenu chunk={chunk} />
                    </div>

                    <p className="ml-7 text-sm text-gray-700">{chunk.content}</p>

                    {chunk.metadata && (
                        <div className="ml-7 mt-2 text-xs text-gray-400">
                            <span className="font-medium">Metadata: </span>

                            {JSON.stringify(chunk.metadata)}
                        </div>
                    )}
                </div>
            ))}

            <KnowledgeBaseDocumentChunkEditDialog knowledgeBaseId={knowledgeBaseId} />

            <KnowledgeBaseDocumentChunkDeleteDialog knowledgeBaseId={knowledgeBaseId} />
        </div>
    );
};

export default KnowledgeBaseDocumentChunkList;
