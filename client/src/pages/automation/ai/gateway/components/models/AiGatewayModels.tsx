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
    useDeleteWorkspaceAiModelMutation,
    useUnpinWorkspaceAiModelMutation,
    useWorkspaceAiGatewayProvidersQuery,
    useWorkspaceAiModelsQuery,
} from '@/shared/middleware/graphql';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useQueryClient} from '@tanstack/react-query';
import {BoxesIcon, PencilIcon, PlusIcon, RotateCcwIcon, TrashIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {AiGatewayModelType} from '../../types';
import AiGatewayModelDialog from './AiGatewayModelDialog';

/**
 * Derives the three catalog-relationship states a model row can be in. `catalogPinned` alone only distinguishes two
 * states; a row can also be unpinned with no matching catalog entry at all (an Azure deployment name, a fine-tune
 * like `ft:gpt-4o:acme:x`, a model newer than the bundled snapshot) — the reconciler skips those silently, so their
 * rates are exactly whatever a human typed, and labeling them "Catalog" would misstate who maintains them.
 */
const getCatalogBadge = (model: AiGatewayModelType): {className: string; label: string} => {
    if (model.catalogPinned) {
        return {
            className: 'bg-surface-warning-secondary text-content-warning-primary',
            label: 'Overridden',
        };
    }

    if (model.catalogManaged) {
        return {
            className: 'bg-surface-success-secondary text-content-success-primary',
            label: 'Catalog',
        };
    }

    return {
        className: 'bg-surface-neutral-secondary text-content-neutral-primary',
        label: 'Unmanaged',
    };
};

const AiGatewayModels = () => {
    const [deletingModelId, setDeletingModelId] = useState<string | undefined>(undefined);
    const [editingModel, setEditingModel] = useState<AiGatewayModelType | undefined>(undefined);
    const [showDialog, setShowDialog] = useState(false);
    const [unpinningModelId, setUnpinningModelId] = useState<string | undefined>(undefined);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    // Model writes (create/update/delete/unpin) require ROLE_ADMIN on WorkspaceAiModelFacadeImpl, so a
    // non-admin member would only discover the denial at submit. Gating on `authenticated` as well as the authority
    // avoids a flash-of-privilege during the logout/re-login transition, when `account` can still carry the prior
    // session's authorities before `getAccount()` reconciles.
    const isAdmin = useAuthenticationStore(
        (state) => state.authenticated && (state.account?.authorities?.includes('ROLE_ADMIN') ?? false)
    );

    const queryClient = useQueryClient();

    const {data: modelsData, isLoading: modelsIsLoading} = useWorkspaceAiModelsQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });
    const {data: providersData, isLoading: providersIsLoading} = useWorkspaceAiGatewayProvidersQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const deleteModelMutation = useDeleteWorkspaceAiModelMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiModels']});

            setDeletingModelId(undefined);
        },
    });
    const unpinModelMutation = useUnpinWorkspaceAiModelMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiModels']});

            setUnpinningModelId(undefined);
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

    const models = modelsData?.workspaceAiModels ?? [];

    const handleConfirmDelete = useCallback(() => {
        if (deletingModelId) {
            deleteModelMutation.mutate({
                modelId: deletingModelId,
                workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
            });
        }
    }, [currentWorkspaceId, deleteModelMutation, deletingModelId]);

    const handleConfirmUnpin = useCallback(() => {
        if (unpinningModelId) {
            unpinModelMutation.mutate({
                modelId: unpinningModelId,
                workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
            });
        }
    }, [currentWorkspaceId, unpinModelMutation, unpinningModelId]);

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
                    button={isAdmin ? <Button label="Add Model" onClick={() => setShowDialog(true)} /> : undefined}
                    icon={<BoxesIcon className="size-12 text-muted-foreground" />}
                    message={
                        isAdmin
                            ? 'Register models from your configured providers.'
                            : 'No models are registered yet. An administrator can add them.'
                    }
                    title="No Models Registered"
                />
            ) : (
                <>
                    {isAdmin && (
                        <div className="mb-4 flex items-center justify-end py-4">
                            <Button
                                icon={<PlusIcon className="size-4" />}
                                label="Add Model"
                                onClick={() => setShowDialog(true)}
                            />
                        </div>
                    )}

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

                                    <th className="pb-2 font-medium">Source</th>

                                    <th className="pb-2 font-medium">Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {models.map((model) => {
                                    if (!model) {
                                        return null;
                                    }

                                    const catalogBadge = getCatalogBadge(model);

                                    return (
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
                                                            ? 'bg-surface-success-secondary text-content-success-primary'
                                                            : 'bg-surface-neutral-secondary text-content-neutral-primary'
                                                    )}
                                                >
                                                    {model.enabled ? 'Active' : 'Disabled'}
                                                </span>
                                            </td>

                                            <td className="py-3">
                                                <span
                                                    className={twMerge(
                                                        'rounded-full px-2 py-0.5 text-xs font-medium',
                                                        catalogBadge.className
                                                    )}
                                                >
                                                    {catalogBadge.label}
                                                </span>
                                            </td>

                                            <td className="py-3">
                                                {isAdmin ? (
                                                    <div className="flex gap-2">
                                                        <button
                                                            className="text-muted-foreground hover:text-foreground"
                                                            onClick={() => handleEditModel(model)}
                                                        >
                                                            <PencilIcon className="size-4" />
                                                        </button>

                                                        {model.catalogPinned && (
                                                            <button
                                                                className="text-muted-foreground hover:text-foreground"
                                                                onClick={() => setUnpinningModelId(model.id)}
                                                                title="Reset to catalog"
                                                            >
                                                                <RotateCcwIcon className="size-4" />
                                                            </button>
                                                        )}

                                                        <button
                                                            className="text-destructive hover:text-destructive/80"
                                                            onClick={() => setDeletingModelId(model.id)}
                                                        >
                                                            <TrashIcon className="size-4" />
                                                        </button>
                                                    </div>
                                                ) : (
                                                    <span className="text-muted-foreground">-</span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })}
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

            <AlertDialog open={!!unpinningModelId}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Reset this model to catalog pricing?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This clears your override. On the next catalog reconcile, the context window, cost rates,
                            and capabilities you set here will be replaced by the catalog's values. This cannot be
                            undone.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setUnpinningModelId(undefined)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction disabled={unpinModelMutation.isPending} onClick={handleConfirmUnpin}>
                            Reset to Catalog
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
