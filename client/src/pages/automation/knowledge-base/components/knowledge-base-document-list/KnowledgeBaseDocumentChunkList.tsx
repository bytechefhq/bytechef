import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import KnowledgeBaseDocumentChunkEditDialog from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkEditDialog';
import KnowledgeBaseDocumentChunkListItemDropdownMenu from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkListItemDropdownMenu';
import KnowledgeBaseDocumentChunkListItemHeader from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkListItemHeader';
import KnowledgeBaseDocumentChunkListSelectionBar from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkListSelectionBar';
import useKnowledgeBaseDocumentChunkDeleteDialog from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentChunkDeleteDialog';
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
    const {
        handleClose: handleDeleteDialogClose,
        handleConfirm: handleDeleteConfirm,
        open: deleteDialogOpen,
    } = useKnowledgeBaseDocumentChunkDeleteDialog({knowledgeBaseId});

    if (chunks.length === 0) {
        return (
            <p className="ml-7 border-l border-stroke-neutral-tertiary py-2 pl-4 text-sm text-muted-foreground">
                No chunks available for this document.
            </p>
        );
    }

    return (
        <div className="ml-7 space-y-2 border-l border-stroke-neutral-tertiary py-2 pl-4">
            <KnowledgeBaseDocumentChunkListSelectionBar />

            {chunks.map((chunk, index) => (
                <div className="rounded-lg border border-stroke-neutral-secondary bg-background p-4" key={chunk.id}>
                    <div className="mb-2 flex items-center justify-between">
                        <KnowledgeBaseDocumentChunkListItemHeader
                            chunkId={chunk.id}
                            chunkIndex={index}
                            documentName={documentName}
                        />

                        <KnowledgeBaseDocumentChunkListItemDropdownMenu chunk={chunk} />
                    </div>

                    <p className="ml-7 text-sm text-content-neutral-primary">{chunk.content}</p>

                    {chunk.metadata && (
                        <p className="ml-7 mt-2 text-xs text-content-neutral-tertiary">
                            <span className="font-medium">Metadata: </span>

                            {JSON.stringify(chunk.metadata)}
                        </p>
                    )}
                </div>
            ))}

            <KnowledgeBaseDocumentChunkEditDialog knowledgeBaseId={knowledgeBaseId} />

            <DeleteAlertDialog
                onCancel={handleDeleteDialogClose}
                onDelete={handleDeleteConfirm}
                open={deleteDialogOpen}
            />
        </div>
    );
};

export default KnowledgeBaseDocumentChunkList;
