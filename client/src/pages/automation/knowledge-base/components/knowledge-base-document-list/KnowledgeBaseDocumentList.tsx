import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {KnowledgeBaseDocument} from '@/shared/middleware/graphql';

import KnowledgeBaseDocumentChunkList from './KnowledgeBaseDocumentChunkList';
import KnowledgeBaseDocumentListItem from './KnowledgeBaseDocumentListItem';
import useKnowledgeBaseDocumentList from './hooks/useKnowledgeBaseDocumentList';
import useKnowledgeBaseDocumentListItemDeleteDialog from './hooks/useKnowledgeBaseDocumentListItemDeleteDialog';

interface KnowledgeBaseDocumentListProps {
    documents: KnowledgeBaseDocument[];
    knowledgeBaseId: string;
}

const KnowledgeBaseDocumentList = ({documents, knowledgeBaseId}: KnowledgeBaseDocumentListProps) => {
    const {getRemainingTagsForDocument, getSortedChunksForDocument, getTagsForDocument} = useKnowledgeBaseDocumentList({
        documents,
    });

    const {
        handleClose: handleDeleteDialogClose,
        handleConfirm: handleDeleteConfirm,
        open: deleteDialogOpen,
    } = useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId});

    return (
        <div className="w-full divide-y divide-stroke-neutral-tertiary">
            {documents.length === 0 ? (
                <p className="rounded-lg border border-stroke-neutral-secondary bg-surface-neutral-secondary p-8 text-center text-muted-foreground">
                    No documents available. Upload documents to get started.
                </p>
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

                    <DeleteAlertDialog
                        onCancel={handleDeleteDialogClose}
                        onDelete={handleDeleteConfirm}
                        open={deleteDialogOpen}
                    />
                </>
            )}
        </div>
    );
};

export default KnowledgeBaseDocumentList;
