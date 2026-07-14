import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {KnowledgeBaseDocument} from '@/shared/middleware/graphql';

import KnowledgeBaseDocumentListRow from './KnowledgeBaseDocumentListRow';
import useKnowledgeBaseDocumentList from './hooks/useKnowledgeBaseDocumentList';
import useKnowledgeBaseDocumentListItemDeleteDialog from './hooks/useKnowledgeBaseDocumentListItemDeleteDialog';

interface KnowledgeBaseDocumentListProps {
    documents: KnowledgeBaseDocument[];
    knowledgeBaseId: string;
}

const KnowledgeBaseDocumentList = ({documents, knowledgeBaseId}: KnowledgeBaseDocumentListProps) => {
    const {getRemainingTagsForDocument, getTagsForDocument} = useKnowledgeBaseDocumentList();

    const {
        handleClose: handleDeleteDialogClose,
        handleConfirm: handleDeleteConfirm,
        open: deleteDialogOpen,
    } = useKnowledgeBaseDocumentListItemDeleteDialog({knowledgeBaseId});

    return (
        <div className="w-full divide-y divide-border/50">
            {documents.length === 0 ? (
                <p className="rounded-lg border border-stroke-neutral-secondary bg-surface-neutral-secondary p-8 text-center text-muted-foreground">
                    No documents available. Upload documents to get started.
                </p>
            ) : (
                <>
                    {documents.map((document) => (
                        <KnowledgeBaseDocumentListRow
                            document={document}
                            key={document.id}
                            knowledgeBaseId={knowledgeBaseId}
                            remainingTags={getRemainingTagsForDocument(document.id)}
                            tags={getTagsForDocument(document.id)}
                        />
                    ))}

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
