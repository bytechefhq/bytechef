import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
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
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

const ConnectedUserSheetTitle = ({connectedUser}: {connectedUser: ConnectedUser}) => {
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
            <div className="flex items-center space-x-2">
                <h3 className={twMerge('text-lg', !connectedUser.enabled && 'text-muted-foreground')}>
                    {connectedUser.name ?? connectedUser.externalId}
                </h3>

                {!connectedUser.enabled && <Badge variant="destructive">Disabled</Badge>}
            </div>

            <div className="mr-6">
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="icon" variant="ghost">
                            <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                        </Button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem
                            onClick={() =>
                                enableConnectedUserMutation.mutate({
                                    enable: !connectedUser.enabled,
                                    id: connectedUser.id!,
                                })
                            }
                        >
                            {connectedUser.enabled ? 'Disable' : 'Enable'}
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem className="text-destructive" onClick={() => setShowDeleteDialog(true)}>
                            Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {showDeleteDialog && (
                <ConnectedUserDeleteDialog
                    connectedUserId={connectedUser.id!}
                    onClose={() => setShowDeleteDialog(false)}
                />
            )}
        </>
    );
};

export default ConnectedUserSheetTitle;
