import Badge from '@/components/Badge/Badge';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AppDeleteDialog from '@/pages/automation/apps/components/AppDeleteDialog';
import AppDialog from '@/pages/automation/apps/components/AppDialog';
import AppStatusSwitch from '@/pages/automation/apps/components/AppStatusSwitch';
import TagList from '@/shared/components/TagList';
import {App, TagInput, useUpdateAppTagsMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {MoreVerticalIcon, PencilIcon, TrashIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';

export interface AppListItemTagI {
    id: string;
    name: string;
}

interface AppListItemProps {
    app: App;
    canManage: boolean;
    ownerName: string;
    tags: AppListItemTagI[];
}

const AppListItem = ({app, canManage, ownerName, tags}: AppListItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const updateAppTagsMutation = useUpdateAppTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['apps']});
            queryClient.invalidateQueries({queryKey: ['app']});
            queryClient.invalidateQueries({queryKey: ['appTags']});
        },
    });

    const appTags = useMemo(
        () =>
            (app.tagIds || []).flatMap((tagId) => {
                const tag = tags.find((appTag) => appTag.id === tagId);

                return tag ? [{id: Number(tag.id), name: tag.name}] : [];
            }),
        [app.tagIds, tags]
    );

    const remainingTags = useMemo(
        () =>
            tags
                .filter((tag) => !(app.tagIds || []).includes(tag.id))
                .map((tag) => ({id: Number(tag.id), name: tag.name})),
        [app.tagIds, tags]
    );

    const instanceCount = app.instances.length + app.foreignInstances.length;

    return (
        <>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover"
                data-testid={`app-row-${app.id}`}
                onClick={() => navigate(`/automation/apps/${app.id}`)}
            >
                <div className="flex flex-1 items-center py-3">
                    <div className="flex-1">
                        <div className="flex min-h-8 items-center justify-between">
                            <div className="flex w-full items-center justify-between">
                                {app.description ? (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <span className="mr-2 text-base font-semibold">{app.name}</span>
                                        </TooltipTrigger>

                                        <TooltipContent>{app.description}</TooltipContent>
                                    </Tooltip>
                                ) : (
                                    <span className="mr-2 text-base font-semibold">{app.name}</span>
                                )}
                            </div>
                        </div>

                        <div className="mt-2 min-h-7 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center gap-3">
                                <Badge
                                    label={instanceCount === 1 ? '1 instance' : `${instanceCount} instances`}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />

                                <span className="text-xs text-content-neutral-secondary">{`Owner: ${ownerName}`}</span>

                                <div onClick={(event) => event.stopPropagation()}>
                                    <TagList
                                        getRequest={(id, nextTags) => ({
                                            id: String(id),
                                            tags: nextTags.map((tag): TagInput =>
                                                tag.id != null ? {id: String(tag.id), name: tag.name} : {name: tag.name}
                                            ),
                                        })}
                                        id={Number(app.id)}
                                        readOnly={!canManage}
                                        remainingTags={remainingTags}
                                        tags={appTags}
                                        updateTagsMutation={updateAppTagsMutation}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div
                            className="flex min-w-52 items-center justify-end"
                            onClick={(event) => event.stopPropagation()}
                        >
                            <AppStatusSwitch app={app} canManage={canManage} />
                        </div>

                        {canManage && (
                            <div onClick={(event) => event.stopPropagation()}>
                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <button
                                            aria-label={`Actions for ${app.name}`}
                                            className="rounded-md p-1 hover:bg-muted"
                                        >
                                            <MoreVerticalIcon className="size-4" />
                                        </button>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end">
                                        <DropdownMenuItem onClick={() => setShowEditDialog(true)}>
                                            <PencilIcon className="mr-2 size-4" />
                                            Edit
                                        </DropdownMenuItem>

                                        <DropdownMenuSeparator />

                                        <DropdownMenuItem
                                            onClick={() => setShowDeleteDialog(true)}
                                            variant="destructive"
                                        >
                                            <TrashIcon className="mr-2 size-4" />
                                            Delete
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {showEditDialog && (
                <AppDialog app={app} canShare={canManage} onOpenChange={setShowEditDialog} open={showEditDialog} />
            )}

            {showDeleteDialog && (
                <AppDeleteDialog app={app} onClose={() => setShowDeleteDialog(false)} open={showDeleteDialog} />
            )}
        </>
    );
};

export default AppListItem;
