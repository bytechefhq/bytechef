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
import AppRotateKeyDialog from '@/pages/automation/apps/components/AppRotateKeyDialog';
import AppStatusSwitch from '@/pages/automation/apps/components/AppStatusSwitch';
import {HOSTING_TARGET_LABELS} from '@/pages/automation/apps/constants';
import {App} from '@/shared/middleware/graphql';
import {KeyRoundIcon, MoreVerticalIcon, PencilIcon, TrashIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

interface AppListItemProps {
    app: App;
    canManage: boolean;
    ownerName: string;
}

const AppListItem = ({app, canManage, ownerName}: AppListItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showRotateDialog, setShowRotateDialog] = useState(false);

    const navigate = useNavigate();

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
                                    label={HOSTING_TARGET_LABELS[app.hostingTarget]}
                                    styleType="secondary-filled"
                                    weight="semibold"
                                />

                                {app.url && <span className="text-xs text-content-neutral-secondary">{app.url}</span>}

                                <span className="text-xs text-content-neutral-secondary">{`Owner: ${ownerName}`}</span>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div
                            className="flex min-w-52 flex-col items-end gap-y-2"
                            onClick={(event) => event.stopPropagation()}
                        >
                            <div className="flex min-h-8 items-center">
                                <AppStatusSwitch app={app} canManage={canManage} />
                            </div>

                            <span className="flex min-h-7 items-center text-xs text-content-neutral-secondary">
                                {app.lastDeployedDate
                                    ? `Last deployed ${new Date(app.lastDeployedDate).toLocaleDateString()}`
                                    : 'Never deployed'}
                            </span>
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

                                        <DropdownMenuItem onClick={() => setShowRotateDialog(true)}>
                                            <KeyRoundIcon className="mr-2 size-4" />
                                            Rotate key
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

            {showEditDialog && <AppDialog app={app} onOpenChange={setShowEditDialog} open={showEditDialog} />}

            {showRotateDialog && (
                <AppRotateKeyDialog app={app} onClose={() => setShowRotateDialog(false)} open={showRotateDialog} />
            )}

            {showDeleteDialog && (
                <AppDeleteDialog app={app} onClose={() => setShowDeleteDialog(false)} open={showDeleteDialog} />
            )}
        </>
    );
};

export default AppListItem;
