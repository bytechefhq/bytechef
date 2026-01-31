import {CollapsibleTrigger} from '@/components/ui/collapsible';
import KnowledgeBaseDocumentListItemDropdownMenu from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentListItemDropdownMenu';
import useKnowledgeBaseDocumentListItem from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentListItem';
import useKnowledgeBaseDocumentListItemTagList from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentListItemTagList';
import TagList from '@/shared/components/TagList';
import {KnowledgeBaseDocument, Tag} from '@/shared/middleware/graphql';
import {ChevronDownIcon} from 'lucide-react';

interface KnowledgeBaseDocumentListItemProps {
    document: KnowledgeBaseDocument;
    remainingTags?: Tag[];
    tags: Tag[];
}

const KnowledgeBaseDocumentListItem = ({document, remainingTags, tags}: KnowledgeBaseDocumentListItemProps) => {
    const {
        chunkCount,
        chunksCollapsibleTriggerRef,
        displayName,
        documentIcon,
        handleDocumentListItemClick,
        handleDocumentListItemKeyDown,
        handleTagListClick,
        statusBadge,
    } = useKnowledgeBaseDocumentListItem({document});

    const {convertedRemainingTags, convertedTags, updateTagsMutation} = useKnowledgeBaseDocumentListItemTagList({
        knowledgeBaseDocumentId: document.id,
        remainingTags,
        tags,
    });

    return (
        <div
            className="flex w-full cursor-pointer items-center justify-between rounded-md px-2 hover:bg-muted/50"
            onClick={handleDocumentListItemClick}
            onKeyDown={handleDocumentListItemKeyDown}
            role="button"
            tabIndex={0}
        >
            <div className="flex flex-1 items-center py-4">
                <div className="flex-1">
                    <div className="flex items-center gap-2">
                        {documentIcon}

                        <span className="text-base font-semibold">{displayName}</span>
                    </div>

                    <div className="mt-2 flex items-center gap-4">
                        <CollapsibleTrigger
                            className="group flex items-center text-xs font-semibold text-muted-foreground"
                            ref={chunksCollapsibleTriggerRef}
                        >
                            <span className="mr-1">
                                {chunkCount === 1 ? `${chunkCount} chunk` : `${chunkCount} chunks`}
                            </span>

                            <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                        </CollapsibleTrigger>

                        {document.createdDate && (
                            <span className="text-xs text-muted-foreground">
                                Created {new Date(document.createdDate).toLocaleDateString()}
                            </span>
                        )}

                        <span onClick={handleTagListClick}>
                            <TagList
                                getRequest={(_id, newTags) => ({
                                    input: {
                                        knowledgeBaseDocumentId: document.id,
                                        tags: newTags.map((tag) => ({id: tag.id, name: tag.name})),
                                    },
                                })}
                                id={+document.id}
                                remainingTags={convertedRemainingTags}
                                tags={convertedTags}
                                updateTagsMutation={updateTagsMutation}
                            />
                        </span>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-x-4">
                    {statusBadge}

                    <KnowledgeBaseDocumentListItemDropdownMenu documentId={document.id} />
                </div>
            </div>
        </div>
    );
};

export default KnowledgeBaseDocumentListItem;
