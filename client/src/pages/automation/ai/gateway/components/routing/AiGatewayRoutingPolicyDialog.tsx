import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {
    AiGatewayRoutingStrategyType,
    useCreateWorkspaceAiGatewayRoutingPolicyMutation,
    useUpdateWorkspaceAiGatewayRoutingPolicyMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
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
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-describedby={undefined} className="max-w-md">
                <DialogHeader>
                    <DialogTitle>{isEditMode ? 'Edit Routing Policy' : 'Add Routing Policy'}</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-gateway-routing-policy-name">
                            Name
                        </Label>

                        <Input
                            id="ai-gateway-routing-policy-name"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="My Routing Policy"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-gateway-routing-policy-strategy">
                            Strategy
                        </Label>

                        <Select
                            onValueChange={(value) => setStrategy(value as AiGatewayRoutingStrategyType)}
                            value={strategy}
                        >
                            <SelectTrigger id="ai-gateway-routing-policy-strategy">
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {STRATEGY_TYPES.map((strategyType) => (
                                    <SelectItem key={strategyType} value={strategyType}>
                                        {strategyType}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-gateway-routing-policy-fallback-model">
                            Fallback Model (optional)
                        </Label>

                        <Input
                            id="ai-gateway-routing-policy-fallback-model"
                            onChange={(event) => setFallbackModel(event.target.value)}
                            placeholder="gpt-4o"
                            value={fallbackModel}
                        />
                    </fieldset>
                </div>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!name || createMutation.isPending || updateMutation.isPending}
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiGatewayRoutingPolicyDialog;
