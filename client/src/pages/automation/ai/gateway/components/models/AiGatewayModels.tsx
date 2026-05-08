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
    useDeleteWorkspaceAiGatewayModelMutation,
    useWorkspaceAiGatewayModelsQuery,
    useWorkspaceAiGatewayProvidersQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {BoxesIcon, PencilIcon, PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {AiGatewayModelType} from '../../types';
import AiGatewayModelDialog from './AiGatewayModelDialog';

const AiGatewayModels = () => {
    const [deletingModelId, setDeletingModelId] = useState<string | undefined>(undefined);
    const [editingModel, setEditingModel] = useState<AiGatewayModelType | undefined>(undefined);
    const [showDialog, setShowDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: modelsData, isLoading: modelsIsLoading} = useWorkspaceAiGatewayModelsQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });
    const {data: providersData, isLoading: providersIsLoading} = useWorkspaceAiGatewayProvidersQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const deleteModelMutation = useDeleteWorkspaceAiGatewayModelMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiGatewayModels']});

            setDeletingModelId(undefined);
        },
    });

    const providerNameMap = useMemo(() => {
        const providerMap = new Map<string, string>();

        for (const provider of providersData?.workspaceAiGatewayProviders ?? []) {
            if (provider) {
                providerMap.set(provider.id, provider.name);
            }
        }

        return providerMap;
    }, [providersData]);

    const models = modelsData?.workspaceAiGatewayModels ?? [];

    const handleConfirmDelete = useCallback(() => {
        if (deletingModelId) {
            deleteModelMutation.mutate({
                modelId: deletingModelId,
                workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
            });
        }
    }, [currentWorkspaceId, deleteModelMutation, deletingModelId]);

    const handleEditModel = useCallback((model: AiGatewayModelType) => {
        setEditingModel(model);
        setShowDialog(true);
    }, []);

    const handleCloseDialog = useCallback(() => {
        setShowDialog(false);
        setEditingModel(undefined);
    }, []);

    if (modelsIsLoading || providersIsLoading) {
        return <PageLoader loading={true} />;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {models.length === 0 ? (
                <EmptyList
                    button={<Button label="Add Model" onClick={() => setShowDialog(true)} />}
                    icon={<BoxesIcon className="size-12 text-muted-foreground" />}
                    message="Register models from your configured providers."
                    title="No Models Registered"
                />
            ) : (
                <>
                    <div className="mb-4 flex items-center justify-end py-4">
                        <Button
                            icon={<PlusIcon className="size-4" />}
                            label="Add Model"
                            onClick={() => setShowDialog(true)}
                        />
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead>
                                <tr className="border-b text-muted-foreground">
                                    <th className="pb-2 font-medium">Name</th>

                                    <th className="pb-2 font-medium">Provider</th>

                                    <th className="pb-2 font-medium">Alias</th>

                                    <th className="pb-2 font-medium">Context Window</th>

                                    <th className="pb-2 font-medium">Input Cost ($/M tokens)</th>

                                    <th className="pb-2 font-medium">Output Cost ($/M tokens)</th>

                                    <th className="pb-2 font-medium">Enabled</th>

                                    <th className="pb-2 font-medium">Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {models.map((model) =>
                                    model ? (
                                        <tr className="border-b" key={model.id}>
                                            <td className="py-3 font-medium">{model.name}</td>

                                            <td className="py-3">
                                                {providerNameMap.get(model.providerId) || model.providerId}
                                            </td>

                                            <td className="py-3 text-muted-foreground">{model.alias || '-'}</td>

                                            <td className="py-3">
                                                {model.contextWindow
                                                    ? `${(model.contextWindow / 1000).toFixed(0)}k`
                                                    : '-'}
                                            </td>

                                            <td className="py-3">
                                                {model.inputCostPerMTokens != null
                                                    ? `$${model.inputCostPerMTokens.toFixed(2)}`
                                                    : '-'}
                                            </td>

                                            <td className="py-3">
                                                {model.outputCostPerMTokens != null
                                                    ? `$${model.outputCostPerMTokens.toFixed(2)}`
                                                    : '-'}
                                            </td>

                                            <td className="py-3">
                                                <span
                                                    className={twMerge(
                                                        'rounded-full px-2 py-0.5 text-xs font-medium',
                                                        model.enabled
                                                            ? 'bg-green-100 text-green-800'
                                                            : 'bg-gray-100 text-gray-800'
                                                    )}
                                                >
                                                    {model.enabled ? 'Active' : 'Disabled'}
                                                </span>
                                            </td>

                                            <td className="py-3">
                                                <div className="flex gap-2">
                                                    <button
                                                        className="text-muted-foreground hover:text-foreground"
                                                        onClick={() => handleEditModel(model)}
                                                    >
                                                        <PencilIcon className="size-4" />
                                                    </button>

                                                    <button
                                                        className="text-destructive hover:text-destructive/80"
                                                        onClick={() => setDeletingModelId(model.id)}
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

            <AlertDialog open={!!deletingModelId}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the model.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setDeletingModelId(undefined)}>Cancel</AlertDialogCancel>

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
                <AiGatewayModelDialog
                    model={editingModel}
                    onClose={handleCloseDialog}
                    workspaceId={String(currentWorkspaceId)}
                />
            )}
        </div>
    );
};

export default AiGatewayModels;
