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

const ContextStoreListItem = ({contextStore, isAdmin, remainingTags, tags}: ContextStoreListItemProps) => {
    const navigate = useNavigate();

    const handleRowClick = () => {
        navigate(`/automation/context-stores/${contextStore.id}`);
    };

    const handleTagListClick = (event: MouseEvent) => {
        event.preventDefault();
        event.stopPropagation();
    };

    return (
        <div
            className="mb-2 flex w-full cursor-pointer items-center justify-between rounded border border-border/50 px-3 hover:bg-muted/50"
            data-testid={`context-store-row-${contextStore.id}`}
            onClick={handleRowClick}
        >
            <div className="flex flex-1 items-center py-3">
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
