import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AppEventDialog from '@/ee/pages/embedded/app-events/components/AppEventDialog';
import {AppEvent} from '@/ee/shared/middleware/embedded/configuration';
import {useDeleteAppEventMutation} from '@/ee/shared/mutations/embedded/appEvents.mutations';
import {AppEventKeys} from '@/ee/shared/queries/embedded/appEvents.queries';
import {UNION_ENVIRONMENT_ID, useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {useQueryClient} from '@tanstack/react-query';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

interface AppEventListItemProps {
    appEvent: AppEvent;
}

const AppEventListItem = ({appEvent}: AppEventListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const canDeleteAppEvent = useHasEmbeddedScope('APP_EVENT_DELETE', UNION_ENVIRONMENT_ID);
    const canEditAppEvent = useHasEmbeddedScope('APP_EVENT_EDIT', UNION_ENVIRONMENT_ID);

    const queryClient = useQueryClient();

    const deleteAppEventMutation = useDeleteAppEventMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: AppEventKeys.appEvents,
            });
        },
    });

    const handleAlertDeleteDialogClick = () => {
        if (appEvent.id) {
            deleteAppEventMutation.mutate(appEvent.id);

            setShowDeleteDialog(false);
        }
    };

    return (
        <li
            className="relative mb-2 flex items-center justify-between rounded border border-border/50 px-3 py-3 hover:bg-surface-neutral-primary-hover"
            key={appEvent.id}
        >
            <div className="flex-1">
                <span className="text-base">{appEvent.name}</span>
            </div>

            <div className="flex justify-end gap-x-6">
                {appEvent.createdDate && (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-content-neutral-secondary">
                            <span>
                                {`${appEvent.createdDate?.toLocaleDateString()} ${appEvent.createdDate?.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Created Date</TooltipContent>
                    </Tooltip>
                )}

                {(canEditAppEvent || canDeleteAppEvent) && (
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            {canEditAppEvent && (
                                <DropdownMenuItem
                                    icon={<EditIcon />}
                                    label="Edit"
                                    onClick={() => setShowEditDialog(true)}
                                />
                            )}

                            {canEditAppEvent && canDeleteAppEvent && <DropdownMenuSeparator />}

                            {canDeleteAppEvent && (
                                <DropdownMenuItem
                                    icon={<Trash2Icon />}
                                    label="Delete"
                                    onClick={() => setShowDeleteDialog(true)}
                                    variant="destructive"
                                />
                            )}
                        </DropdownMenuContent>
                    </DropdownMenu>
                )}
            </div>

            <AlertDialog
                description="This action cannot be undone. This will permanently delete the app event."
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleAlertDeleteDialogClick}
                open={showDeleteDialog}
            />

            {showEditDialog && <AppEventDialog appEvent={appEvent} onClose={() => setShowEditDialog(false)} />}
        </li>
    );
};

export default AppEventListItem;
