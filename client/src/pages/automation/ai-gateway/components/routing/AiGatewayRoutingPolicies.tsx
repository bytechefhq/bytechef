import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useDeleteWorkspaceAiGatewayRoutingPolicyMutation,
    useWorkspaceAiGatewayRoutingPoliciesQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PencilIcon, PlusIcon, RouteIcon, TrashIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {AiGatewayRoutingPolicyType} from '../../types';
import AiGatewayRoutingPolicyDialog from './AiGatewayRoutingPolicyDialog';

const AiGatewayRoutingPolicies = () => {
    const [deletingPolicyId, setDeletingPolicyId] = useState<string | undefined>(undefined);
    const [editingPolicy, setEditingPolicy] = useState<AiGatewayRoutingPolicyType | undefined>(undefined);
    const [showDialog, setShowDialog] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const {data: policiesData, isLoading: policiesIsLoading} = useWorkspaceAiGatewayRoutingPoliciesQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const deleteRoutingPolicyMutation = useDeleteWorkspaceAiGatewayRoutingPolicyMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiGatewayRoutingPolicies']});

            setDeletingPolicyId(undefined);
        },
    });

    const policies = policiesData?.workspaceAiGatewayRoutingPolicies ?? [];

    const handleConfirmDelete = useCallback(() => {
        if (deletingPolicyId) {
            deleteRoutingPolicyMutation.mutate({
                routingPolicyId: deletingPolicyId,
                workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
            });
        }
    }, [currentWorkspaceId, deletingPolicyId, deleteRoutingPolicyMutation]);

    const handleEditPolicy = useCallback((policy: AiGatewayRoutingPolicyType) => {
        setEditingPolicy(policy);
        setShowDialog(true);
    }, []);

    const handleCloseDialog = useCallback(() => {
        setShowDialog(false);
        setEditingPolicy(undefined);
    }, []);

    if (policiesIsLoading) {
        return <PageLoader loading={true} />;
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {policies.length === 0 ? (
                <EmptyList
                    button={<Button label="Add Policy" onClick={() => setShowDialog(true)} />}
                    icon={<RouteIcon className="size-12 text-muted-foreground" />}
                    message="Create routing policies to control how requests are distributed across models."
                    title="No Routing Policies"
                />
            ) : (
                <>
                    <div className="mb-4 flex items-center justify-end py-4">
                        <Button
                            icon={<PlusIcon className="size-4" />}
                            label="Add Policy"
                            onClick={() => setShowDialog(true)}
                        />
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead>
                                <tr className="border-b text-muted-foreground">
                                    <th className="pb-2 font-medium">Name</th>

                                    <th className="pb-2 font-medium">Strategy</th>

                                    <th className="pb-2 font-medium">Deployments</th>

                                    <th className="pb-2 font-medium">Enabled</th>

                                    <th className="pb-2 font-medium">Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {policies.map((policy) =>
                                    policy ? (
                                        <tr className="border-b" key={policy.id}>
                                            <td className="py-3 font-medium">{policy.name}</td>

                                            <td className="py-3">{policy.strategy}</td>

                                            <td className="py-3">{policy.deployments?.filter(Boolean).length ?? 0}</td>

                                            <td className="py-3">
                                                <span
                                                    className={twMerge(
                                                        'rounded-full px-2 py-0.5 text-xs font-medium',
                                                        policy.enabled
                                                            ? 'bg-green-100 text-green-800'
                                                            : 'bg-gray-100 text-gray-800'
                                                    )}
                                                >
                                                    {policy.enabled ? 'Active' : 'Disabled'}
                                                </span>
                                            </td>

                                            <td className="py-3">
                                                <div className="flex gap-2">
                                                    <button
                                                        className="text-muted-foreground hover:text-foreground"
                                                        onClick={() => handleEditPolicy(policy)}
                                                    >
                                                        <PencilIcon className="size-4" />
                                                    </button>

                                                    <button
                                                        className="text-destructive hover:text-destructive/80"
                                                        onClick={() => setDeletingPolicyId(policy.id)}
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

            <DeleteAlertDialog
                onCancel={() => setDeletingPolicyId(undefined)}
                onDelete={handleConfirmDelete}
                open={!!deletingPolicyId}
            />

            {showDialog && currentWorkspaceId != null && (
                <AiGatewayRoutingPolicyDialog
                    onClose={handleCloseDialog}
                    routingPolicy={editingPolicy}
                    workspaceId={String(currentWorkspaceId)}
                />
            )}
        </div>
    );
};

export default AiGatewayRoutingPolicies;
