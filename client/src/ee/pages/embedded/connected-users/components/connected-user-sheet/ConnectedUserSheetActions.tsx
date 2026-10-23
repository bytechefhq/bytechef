import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import ConnectedUserDeleteDialog from '@/ee/pages/embedded/connected-users/components/ConnectedUserDeleteDialog';
import ConnectedUserEditDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserEditDialog';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {useEnableConnectedUserMutation} from '@/ee/shared/mutations/embedded/connectedUsers.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon, PencilIcon, PowerIcon, PowerOffIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

const ConnectedUserSheetActions = ({connectedUser}: {connectedUser: ConnectedUser}) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const queryClient = useQueryClient();

    const enableConnectedUserMutation = useEnableConnectedUserMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectedUserKeys.connectedUsers,
            });
        },
    });

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
                    <DropdownMenuItem className="dropdown-menu-item" onClick={() => setShowEditDialog(true)}>
                        <PencilIcon /> Edit
                    </DropdownMenuItem>

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

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={() => setShowDeleteDialog(true)}
                        variant="destructive"
                    />
                </DropdownMenuContent>
            </DropdownMenu>

            {showEditDialog && (
                <ConnectedUserEditDialog connectedUser={connectedUser} onClose={() => setShowEditDialog(false)} />
            )}

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
