import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import ConnectedUserDeleteDialog from '@/ee/pages/embedded/connected-users/components/ConnectedUserDeleteDialog';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {useEnableConnectedUserMutation} from '@/ee/shared/mutations/embedded/connectedUsers.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon, PowerIcon, PowerOffIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

const ConnectedUserSheetActions = ({connectedUser}: {connectedUser: ConnectedUser}) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

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
                    <DropdownMenuItem
                        className="dropdown-menu-item"
                        onClick={() =>
                            enableConnectedUserMutation.mutate({
                                enable: !connectedUser.enabled,
                                id: connectedUser.id!,
                            })
                        }
                    >
                        {connectedUser.enabled ? (
                            <>
                                <PowerOffIcon /> Disable
                            </>
                        ) : (
                            <>
                                <PowerIcon /> Enable
                            </>
                        )}
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        className="dropdown-menu-item-destructive"
                        onClick={() => setShowDeleteDialog(true)}
                        variant="destructive"
                    >
                        <Trash2Icon /> Delete
                    </DropdownMenuItem>
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
