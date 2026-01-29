import {CollapsibleTrigger} from '@/components/ui/collapsible';
import KnowledgeBaseDocumentListItemDropdownMenu from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentListItemDropdownMenu';
import KnowledgeBaseDocumentListItemTagList from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentListItemTagList';
import useKnowledgeBaseDocumentListItem from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentListItem';
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
                            <div className="mr-1">
                                {chunkCount === 1 ? `${chunkCount} chunk` : `${chunkCount} chunks`}
                            </div>

                            <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                        </CollapsibleTrigger>

                        {document.createdDate && (
                            <span className="text-xs text-muted-foreground">
                                Created {new Date(document.createdDate).toLocaleDateString()}
                            </span>
                        )}

                        <div onClick={handleTagListClick}>
                            <KnowledgeBaseDocumentListItemTagList
                                knowledgeBaseDocumentId={document.id}
                                remainingTags={remainingTags}
                                tags={tags}
                            />
                        </div>
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
