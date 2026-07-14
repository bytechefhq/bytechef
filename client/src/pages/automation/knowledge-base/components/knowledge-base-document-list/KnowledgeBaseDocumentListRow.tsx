import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import KnowledgeBaseDocumentChunkList from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentChunkList';
import KnowledgeBaseDocumentListItem from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentListItem';
import {KnowledgeBaseDocument, useKnowledgeBaseDocumentChunksQuery} from '@/shared/middleware/graphql';
import {useMemo, useState} from 'react';

interface KnowledgeBaseDocumentListRowProps {
    document: KnowledgeBaseDocument;
    knowledgeBaseId: string;
    remainingTags: string[];
    tags: string[];
}

const KnowledgeBaseDocumentListRow = ({
    document,
    knowledgeBaseId,
    remainingTags,
    tags,
}: KnowledgeBaseDocumentListRowProps) => {
    const [open, setOpen] = useState(false);

    const {data, isLoading} = useKnowledgeBaseDocumentChunksQuery({id: document.id}, {enabled: open});

    const sortedChunks = useMemo(
        () =>
            (data?.knowledgeBaseDocumentChunks ?? [])
                .filter((chunk): chunk is NonNullable<typeof chunk> => chunk !== null)
                .sort((chunkA, chunkB) => chunkA.id.localeCompare(chunkB.id)),
        [data?.knowledgeBaseDocumentChunks]
    );

    return (
        <Collapsible className="group" onOpenChange={setOpen} open={open}>
            <KnowledgeBaseDocumentListItem document={document} remainingTags={remainingTags} tags={tags} />

            <CollapsibleContent>
                {isLoading ? (
                    <p className="ml-7 border-l border-border/50 py-2 pl-4 text-sm text-muted-foreground">
                        Loading chunks...
                    </p>
                ) : (
                    <KnowledgeBaseDocumentChunkList
                        chunks={sortedChunks}
                        documentName={document.name}
                        knowledgeBaseId={knowledgeBaseId}
                    />
                )}
            </CollapsibleContent>
        </Collapsible>
    );
};

export default KnowledgeBaseDocumentListRow;
