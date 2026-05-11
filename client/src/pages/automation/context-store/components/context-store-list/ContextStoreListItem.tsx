import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ContextStoreRowActionsMenu from '@/pages/automation/context-store/components/ContextStoreRowActionsMenu';
import ContextStoreListItemTagList from '@/pages/automation/context-store/components/context-store-list/ContextStoreListItemTagList';
import {ContextStore} from '@/shared/middleware/graphql';
import {LayersIcon} from 'lucide-react';
import {MouseEvent} from 'react';
import {useNavigate} from 'react-router-dom';

interface ContextStoreListItemProps {
    contextStore: ContextStore;
    isAdmin: boolean;
    remainingTags: {id: number; name: string}[];
    tags: {id: number; name: string}[];
}

/**
 * Single-row list item for a {@link ContextStore}. Mirrors {@code KnowledgeBaseListItem}'s shape (icon + name with
 * description tooltip, meta row below, actions menu on the right). The whole row is clickable and navigates to the
 * store-detail page where its sources live.
 */
const ContextStoreListItem = ({contextStore, isAdmin, remainingTags, tags}: ContextStoreListItemProps) => {
    const navigate = useNavigate();

    const handleRowClick = () => {
        navigate(`/automation/context-stores/${contextStore.id}`);
    };

    const handleTagListClick = (event: MouseEvent) => {
        // The TagList's combobox stops propagation on its own, but the wrapper still bubbles. Halt here so a
        // tag-add click never navigates to the detail page.
        event.preventDefault();
        event.stopPropagation();
    };

    return (
        <div
            className="flex w-full cursor-pointer items-center justify-between rounded-md px-2 hover:bg-muted/50"
            data-testid={`context-store-row-${contextStore.id}`}
            onClick={handleRowClick}
        >
            <div className="flex flex-1 items-center py-5">
                <div className="flex-1">
                    <div className="flex items-center gap-2">
                        <LayersIcon className="size-5 text-muted-foreground" />

                        {contextStore.description ? (
                            <Tooltip>
                                <TooltipTrigger>
                                    <span className="text-base font-semibold">{contextStore.name}</span>
                                </TooltipTrigger>

                                <TooltipContent>{contextStore.description}</TooltipContent>
                            </Tooltip>
                        ) : (
                            <span className="text-base font-semibold">{contextStore.name}</span>
                        )}
                    </div>

                    <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                        <div className="flex items-center gap-3">
                            <div onClick={handleTagListClick}>
                                <ContextStoreListItemTagList
                                    contextStoreId={contextStore.id}
                                    remainingTags={remainingTags}
                                    tags={tags}
                                />
                            </div>
                        </div>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-x-6" onClick={(event) => event.stopPropagation()}>
                    <ContextStoreRowActionsMenu contextStore={contextStore} isAdmin={isAdmin} />
                </div>
            </div>
        </div>
    );
};

export default ContextStoreListItem;
