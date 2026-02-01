import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import EditKnowledgeBaseDialog from '@/pages/automation/knowledge-base/components/EditKnowledgeBaseDialog';
import KnowledgeBaseListItemDeleteDialog from '@/pages/automation/knowledge-bases/components/knowledge-base-list/KnowledgeBaseListItemDeleteDialog';
import KnowledgeBaseListItemTagList from '@/pages/automation/knowledge-bases/components/knowledge-base-list/KnowledgeBaseListItemTagList';
import useKnowledgeBaseListItem from '@/pages/automation/knowledge-bases/components/knowledge-base-list/hooks/useKnowledgeBaseListItem';
import {KnowledgeBase, Tag} from '@/shared/middleware/graphql';
import {DatabaseIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';

interface KnowledgeBaseListItemProps {
    knowledgeBase: KnowledgeBase;
    remainingTags?: Tag[];
    tags: Tag[];
}

const KnowledgeBaseListItem = ({knowledgeBase, remainingTags, tags}: KnowledgeBaseListItemProps) => {
    const {
        handleCloseDeleteDialog,
        handleEditClick,
        handleEditDialogOpenChange,
        handleKnowledgeBaseClick,
        handleShowDeleteDialog,
        handleTagListClick,
        showDeleteDialog,
        showEditDialog,
    } = useKnowledgeBaseListItem({knowledgeBase});

    return (
        <>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-2 hover:bg-muted/50"
                onClick={handleKnowledgeBaseClick}
            >
                <div className="flex flex-1 items-center py-5">
                    <div className="flex-1">
                        <div className="flex items-center gap-2">
                            <DatabaseIcon className="size-5 text-muted-foreground" />

                            {knowledgeBase.description ? (
                                <Tooltip>
                                    <TooltipTrigger>
                                        <span className="text-base font-semibold">{knowledgeBase.name}</span>
                                    </TooltipTrigger>

                                    <TooltipContent>{knowledgeBase.description}</TooltipContent>
                                </Tooltip>
                            ) : (
                                <span className="text-base font-semibold">{knowledgeBase.name}</span>
                            )}
                        </div>

                        <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center gap-3">
                                <div className="flex items-center gap-4 text-xs text-muted-foreground">
                                    {knowledgeBase.maxChunkSize && <span>Max chunk: {knowledgeBase.maxChunkSize}</span>}

                                    {knowledgeBase.overlap !== undefined && (
                                        <span>Overlap: {knowledgeBase.overlap}</span>
                                    )}
                                </div>

                                <div onClick={handleTagListClick}>
                                    <KnowledgeBaseListItemTagList
                                        knowledgeBaseId={knowledgeBase.id}
                                        remainingTags={remainingTags}
                                        tags={tags}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex flex-col items-end gap-y-1">
                            <Tooltip>
                                <TooltipTrigger>
                                    <div className="text-sm text-muted-foreground">
                                        {knowledgeBase.lastModifiedDate ? (
                                            <span className="text-xs">
                                                {`Modified ${new Date(knowledgeBase.lastModifiedDate).toLocaleDateString()} ${new Date(knowledgeBase.lastModifiedDate).toLocaleTimeString()}`}
                                            </span>
                                        ) : (
                                            <span className="text-xs">Never modified</span>
                                        )}
                                    </div>
                                </TooltipTrigger>

                                <TooltipContent>Last Modified Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild onClick={(event) => event.stopPropagation()}>
                                <Button
                                    aria-label="More KnowledgeBase Actions"
                                    icon={<EllipsisVerticalIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end" className="p-0">
                                <DropdownMenuItem className="dropdown-menu-item" onClick={handleEditClick}>
                                    <EditIcon /> Edit
                                </DropdownMenuItem>

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

            <KnowledgeBaseListItemDeleteDialog
                knowledgeBaseId={knowledgeBase.id}
                onClose={handleCloseDeleteDialog}
                open={showDeleteDialog}
            />

            <EditKnowledgeBaseDialog
                knowledgeBase={knowledgeBase}
                onOpenChange={handleEditDialogOpenChange}
                open={showEditDialog}
            />
        </>
    );
};

export default KnowledgeBaseListItem;
