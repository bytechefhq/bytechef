import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useAiObservabilityNotificationChannelsQuery,
    useDeleteAiObservabilityNotificationChannelMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {MailIcon, PencilIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {AiObservabilityNotificationChannelType} from '../../types';
import AiObservabilityNotificationChannelDialog from './AiObservabilityNotificationChannelDialog';

const AiObservabilityNotificationChannels = () => {
    const [deletingChannelId, setDeletingChannelId] = useState<string | undefined>(undefined);
    const [editingChannel, setEditingChannel] = useState<AiObservabilityNotificationChannelType | undefined>(undefined);
    const [showDialog, setShowDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: channelsData, isLoading: channelsIsLoading} = useAiObservabilityNotificationChannelsQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const deleteChannelMutation = useDeleteAiObservabilityNotificationChannelMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiObservabilityNotificationChannels']});

            setDeletingChannelId(undefined);
        },
    });

    const channels = channelsData?.aiObservabilityNotificationChannels ?? [];

    const handleCloseDialog = useCallback(() => {
        setShowDialog(false);
        setEditingChannel(undefined);
    }, []);

    const handleConfirmDelete = useCallback(() => {
        if (deletingChannelId) {
            deleteChannelMutation.mutate({id: deletingChannelId});
        }
    }, [deleteChannelMutation, deletingChannelId]);

    const handleEditChannel = useCallback((channel: AiObservabilityNotificationChannelType) => {
        setEditingChannel(channel);
        setShowDialog(true);
    }, []);

    if (channelsIsLoading) {
        return <PageLoader loading={true} />;
    }

    return (
        <div>
            {channels.length === 0 ? (
                <EmptyList
                    button={<Button label="Add Channel" onClick={() => setShowDialog(true)} />}
                    icon={<MailIcon className="size-12 text-muted-foreground" />}
                    message="Configure notification channels to receive alert notifications."
                    title="No Notification Channels"
                />
            ) : (
                <>
                    <div className="mb-4 flex items-center justify-end">
                        <Button
                            icon={<PlusIcon className="size-4" />}
                            label="Add Channel"
                            onClick={() => setShowDialog(true)}
                        />
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead>
                                <tr className="border-b text-muted-foreground">
                                    <th className="pb-2 font-medium">Name</th>

                                    <th className="pb-2 font-medium">Type</th>

                                    <th className="pb-2 font-medium">Enabled</th>

                                    <th className="pb-2 font-medium">Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {channels.map((channel) =>
                                    channel ? (
                                        <tr className="border-b" key={channel.id}>
                                            <td className="py-3 font-medium">{channel.name}</td>

                                            <td className="py-3">
                                                <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-800">
                                                    {channel.type}
                                                </span>
                                            </td>

                                            <td className="py-3">
                                                <span
                                                    className={twMerge(
                                                        'rounded-full px-2 py-0.5 text-xs font-medium',
                                                        channel.enabled
                                                            ? 'bg-green-100 text-green-800'
                                                            : 'bg-gray-100 text-gray-800'
                                                    )}
                                                >
                                                    {channel.enabled ? 'Active' : 'Disabled'}
                                                </span>
                                            </td>

                                            <td className="py-3">
                                                <div className="flex gap-2">
                                                    <button
                                                        className="text-muted-foreground hover:text-foreground"
                                                        onClick={() => handleEditChannel(channel)}
                                                    >
                                                        <PencilIcon className="size-4" />
                                                    </button>

                                                    <button
                                                        className="text-destructive hover:text-destructive/80"
                                                        onClick={() => setDeletingChannelId(channel.id)}
                                                    >
                                                        <TrashIcon className="size-4" />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ) : null
                                )}
                            </tbody>
                        </table>
                    </div>
                </>
            )}

            <AlertDialog open={!!deletingChannelId}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the notification channel.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setDeletingChannelId(undefined)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                            onClick={handleConfirmDelete}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showDialog && currentWorkspaceId != null && (
                <AiObservabilityNotificationChannelDialog
                    channel={editingChannel}
                    onClose={handleCloseDialog}
                    workspaceId={String(currentWorkspaceId)}
                />
            )}
        </div>
    );
};

export default AiObservabilityNotificationChannels;
