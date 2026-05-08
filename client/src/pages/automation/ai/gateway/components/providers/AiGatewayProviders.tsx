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
    useDeleteWorkspaceAiGatewayProviderMutation,
    useWorkspaceAiGatewayProvidersQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {BrainCircuitIcon, PencilIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {AiGatewayProviderType} from '../../types';
import AiGatewayProviderDialog from './AiGatewayProviderDialog';

const AiGatewayProviders = () => {
    const [deletingProviderId, setDeletingProviderId] = useState<string | undefined>(undefined);
    const [editingProvider, setEditingProvider] = useState<AiGatewayProviderType | undefined>(undefined);
    const [showDialog, setShowDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: providersData, isLoading: providersIsLoading} = useWorkspaceAiGatewayProvidersQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const deleteProviderMutation = useDeleteWorkspaceAiGatewayProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiGatewayProviders']});

            setDeletingProviderId(undefined);
        },
    });

    const providers = providersData?.workspaceAiGatewayProviders ?? [];

    const handleConfirmDelete = useCallback(() => {
        if (deletingProviderId) {
            deleteProviderMutation.mutate({
                providerId: deletingProviderId,
                workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
            });
        }
    }, [currentWorkspaceId, deleteProviderMutation, deletingProviderId]);

    const handleEditProvider = useCallback((provider: AiGatewayProviderType) => {
        setEditingProvider(provider);
        setShowDialog(true);
    }, []);

    const handleCloseDialog = useCallback(() => {
        setShowDialog(false);
        setEditingProvider(undefined);
    }, []);

    if (providersIsLoading) {
        return <PageLoader loading={true} />;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {providers.length === 0 ? (
                <EmptyList
                    button={<Button label="Add Provider" onClick={() => setShowDialog(true)} />}
                    icon={<BrainCircuitIcon className="size-12 text-muted-foreground" />}
                    message="Configure LLM providers to get started with the LLM Gateway."
                    title="No Providers Configured"
                />
            ) : (
                <>
                    <div className="mb-4 flex items-center justify-end py-4">
                        <Button
                            icon={<PlusIcon className="size-4" />}
                            label="Add Provider"
                            onClick={() => setShowDialog(true)}
                        />
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead>
                                <tr className="border-b text-muted-foreground">
                                    <th className="pb-2 font-medium">Name</th>

                                    <th className="pb-2 font-medium">Type</th>

                                    <th className="pb-2 font-medium">Base URL</th>

                                    <th className="pb-2 font-medium">Enabled</th>

                                    <th className="pb-2 font-medium">Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {providers.map((provider) =>
                                    provider ? (
                                        <tr className="border-b" key={provider.id}>
                                            <td className="py-3 font-medium">{provider.name}</td>

                                            <td className="py-3">{provider.type}</td>

                                            <td className="py-3 text-muted-foreground">
                                                {provider.baseUrl || 'Default'}
                                            </td>

                                            <td className="py-3">
                                                <span
                                                    className={twMerge(
                                                        'rounded-full px-2 py-0.5 text-xs font-medium',
                                                        provider.enabled
                                                            ? 'bg-green-100 text-green-800'
                                                            : 'bg-gray-100 text-gray-800'
                                                    )}
                                                >
                                                    {provider.enabled ? 'Active' : 'Disabled'}
                                                </span>
                                            </td>

                                            <td className="py-3">
                                                <div className="flex gap-2">
                                                    <button
                                                        className="text-muted-foreground hover:text-foreground"
                                                        onClick={() => handleEditProvider(provider)}
                                                    >
                                                        <PencilIcon className="size-4" />
                                                    </button>

                                                    <button
                                                        className="text-destructive hover:text-destructive/80"
                                                        onClick={() => setDeletingProviderId(provider.id)}
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

            <AlertDialog open={!!deletingProviderId}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the provider and all its
                            associated models.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setDeletingProviderId(undefined)}>Cancel</AlertDialogCancel>

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
                <AiGatewayProviderDialog
                    onClose={handleCloseDialog}
                    provider={editingProvider}
                    workspaceId={String(currentWorkspaceId)}
                />
            )}
        </div>
    );
};

export default AiGatewayProviders;
