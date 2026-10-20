import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import ConnectedUserDeleteDialog from '@/ee/pages/embedded/connected-users/components/ConnectedUserDeleteDialog';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {useEnableConnectedUserMutation} from '@/ee/shared/mutations/embedded/connectedUsers.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon, PowerIcon, PowerOffIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

const ConnectedUserSheetActions = ({connectedUser}: {connectedUser: ConnectedUser}) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const canDeleteConnectedUser = useHasEmbeddedScope('CONNECTED_USER_DELETE');
    const canManageConnectedUser = useHasEmbeddedScope('CONNECTED_USER_MANAGE');

    const queryClient = useQueryClient();

    const enableConnectedUserMutation = useEnableConnectedUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUsers,
            });
        },
    });

    if (!canManageConnectedUser && !canDeleteConnectedUser) {
        return null;
    }

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                        size="icon"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    {canManageConnectedUser && (
                        <DropdownMenuItem
                            icon={connectedUser.enabled ? <PowerOffIcon /> : <PowerIcon />}
                            label={connectedUser.enabled ? 'Disable' : 'Enable'}
                            onClick={() =>
                                enableConnectedUserMutation.mutate({
                                    enable: !connectedUser.enabled,
                                    id: connectedUser.id!,
                                })
                            }
                        />
                    )}

                    {canManageConnectedUser && canDeleteConnectedUser && <DropdownMenuSeparator />}

                    {canDeleteConnectedUser && (
                        <DropdownMenuItem
                            icon={<Trash2Icon />}
                            label="Delete"
                            onClick={() => setShowDeleteDialog(true)}
                            variant="destructive"
                        />
                    )}
                </DropdownMenuContent>
            </DropdownMenu>

            {showDeleteDialog && (
                <ConnectedUserDeleteDialog
                    connectedUserId={connectedUser.id!}
                    onClose={() => setShowDeleteDialog(false)}
                />
            )}
        </>
    );
};

export default ConnectedUserSheetActions;
