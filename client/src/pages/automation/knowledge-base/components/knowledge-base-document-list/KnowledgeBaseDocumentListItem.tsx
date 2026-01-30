import Button from '@/components/Button/Button';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import KnowledgeBaseDocumentListItemDeleteDialog from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentListItemDeleteDialog';
import KnowledgeBaseDocumentListItemTagList from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentListItemTagList';
import useKnowledgeBaseDocumentListItem from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentListItem';
import {KnowledgeBaseDocument, Tag} from '@/shared/middleware/graphql';
import {ChevronDownIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface KnowledgeBaseDocumentListItemProps {
    document: KnowledgeBaseDocument;
    knowledgeBaseId: string;
    remainingTags?: Tag[];
    tags: Tag[];
}

const KnowledgeBaseDocumentListItem = ({
    document,
    knowledgeBaseId,
    remainingTags,
    tags,
}: KnowledgeBaseDocumentListItemProps) => {
    const {
        chunkCount,
        chunksCollapsibleTriggerRef,
        currentStatus,
        displayName,
        documentIcon,
        getStatusBadge,
        handleCloseDeleteDialog,
        handleDocumentListItemClick,
        handleShowDeleteDialog,
        showDeleteDialog,
    } = useKnowledgeBaseDocumentListItem({document});

    const handleTagListClick = (event: React.MouseEvent) => {
        event.stopPropagation();
    };

    return (
        <>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-2 hover:bg-muted/50"
                onClick={handleDocumentListItemClick}
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
                        {getStatusBadge(currentStatus)}

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild onClick={(event) => event.stopPropagation()}>
                                <Button
                                    aria-label="More Document Actions"
                                    icon={<EllipsisVerticalIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end" className="p-0">
                                <DropdownMenuSeparator className="m-0" />

                                <DropdownMenuItem
                                    className="dropdown-menu-item-destructive"
                                    onClick={(event) => {
                                        event.stopPropagation();

                                        handleShowDeleteDialog();
                                    }}
                                >
                                    <Trash2Icon /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <KnowledgeBaseDocumentListItemDeleteDialog
                documentId={document.id}
                knowledgeBaseId={knowledgeBaseId}
                onClose={handleCloseDeleteDialog}
                open={showDeleteDialog}
            />
        </>
    );
};

export default KnowledgeBaseDocumentListItem;
