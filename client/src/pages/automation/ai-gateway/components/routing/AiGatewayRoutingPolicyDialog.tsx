import Button from '@/components/Button/Button';
import {
    AiGatewayRoutingStrategyType,
    useCreateWorkspaceAiGatewayRoutingPolicyMutation,
    useUpdateWorkspaceAiGatewayRoutingPolicyMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

import {AiGatewayRoutingPolicyType} from '../../types';

interface AiGatewayRoutingPolicyDialogProps {
    onClose: () => void;
    routingPolicy?: AiGatewayRoutingPolicyType;
    workspaceId: string;
}

const STRATEGY_TYPES: AiGatewayRoutingStrategyType[] = [
    AiGatewayRoutingStrategyType.Simple,
    AiGatewayRoutingStrategyType.WeightedRandom,
    AiGatewayRoutingStrategyType.CostOptimized,
    AiGatewayRoutingStrategyType.LatencyOptimized,
    AiGatewayRoutingStrategyType.PriorityFallback,
    AiGatewayRoutingStrategyType.IntelligentBalanced,
    AiGatewayRoutingStrategyType.IntelligentCost,
    AiGatewayRoutingStrategyType.IntelligentQuality,
    AiGatewayRoutingStrategyType.TagBased,
];

const AiGatewayRoutingPolicyDialog = ({onClose, routingPolicy, workspaceId}: AiGatewayRoutingPolicyDialogProps) => {
    const [fallbackModel, setFallbackModel] = useState(routingPolicy?.fallbackModel ?? '');
    const [name, setName] = useState(routingPolicy?.name ?? '');
    const [strategy, setStrategy] = useState<AiGatewayRoutingStrategyType>(
        routingPolicy?.strategy ?? AiGatewayRoutingStrategyType.Simple
    );

    const queryClient = useQueryClient();

    const isEditMode = !!routingPolicy;

    const createMutation = useCreateWorkspaceAiGatewayRoutingPolicyMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiGatewayRoutingPolicies']});

            onClose();
        },
    });

    const updateMutation = useUpdateWorkspaceAiGatewayRoutingPolicyMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiGatewayRoutingPolicies']});

            onClose();
        },
    });

    const handleSubmit = useCallback(() => {
        if (isEditMode) {
            updateMutation.mutate({
                id: routingPolicy.id,
                input: {
                    fallbackModel: fallbackModel || undefined,
                    name,
                    strategy,
                },
                workspaceId,
            });
        } else {
            createMutation.mutate({
                input: {
                    fallbackModel: fallbackModel || undefined,
                    name,
                    strategy,
                    workspaceId,
                },
            });
        }
    }, [createMutation, fallbackModel, isEditMode, name, routingPolicy, strategy, updateMutation, workspaceId]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">{isEditMode ? 'Edit Routing Policy' : 'Add Routing Policy'}</h3>

                    <button onClick={onClose}>
                        <XIcon className="size-4" />
                    </button>
                </div>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Name</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="My Routing Policy"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Strategy</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setStrategy(event.target.value as AiGatewayRoutingStrategyType)}
                            value={strategy}
                        >
                            {STRATEGY_TYPES.map((strategyType) => (
                                <option key={strategyType} value={strategyType}>
                                    {strategyType}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Fallback Model (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setFallbackModel(event.target.value)}
                            placeholder="gpt-4o"
                            value={fallbackModel}
                        />
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!name || createMutation.isPending || updateMutation.isPending}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        </div>
    );
};

export default AiGatewayRoutingPolicyDialog;
