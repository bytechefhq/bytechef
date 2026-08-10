import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {
    useCreateWorkspaceAiModelMutation,
    useUpdateWorkspaceAiModelMutation,
    useWorkspaceAiGatewayProvidersQuery,
    useWorkspaceAiGatewayRoutingPoliciesQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useState} from 'react';

import {AiGatewayModelType} from '../../types';

interface AiGatewayModelDialogProps {
    model?: AiGatewayModelType;
    onClose: () => void;
    workspaceId: string;
}

/**
 * Sentinel value for the "Inherit from workspace/system default" row. Radix's `SelectItem` rejects an
 * empty string as a value, and a `SelectValue` placeholder is not a selectable row — so a model that
 * already has a routing policy could not otherwise be put back on the inherited default.
 */
const INHERIT_ROUTING_POLICY_VALUE = '__INHERIT__';

const AiGatewayModelDialog = ({model, onClose, workspaceId}: AiGatewayModelDialogProps) => {
    const [alias, setAlias] = useState(model?.alias ?? '');
    const [capabilities, setCapabilities] = useState(model?.capabilities ?? '');
    const [contextWindow, setContextWindow] = useState(model?.contextWindow?.toString() ?? '');
    const [inputCostPerMTokens, setInputCostPerMTokens] = useState(model?.inputCostPerMTokens?.toString() ?? '');
    const [name, setName] = useState(model?.name ?? '');
    const [outputCostPerMTokens, setOutputCostPerMTokens] = useState(model?.outputCostPerMTokens?.toString() ?? '');
    const [providerId, setProviderId] = useState(model?.providerId ?? '');
    const [defaultRoutingPolicyId, setDefaultRoutingPolicyId] = useState(model?.defaultRoutingPolicyId ?? '');

    const queryClient = useQueryClient();

    const {data: providersData} = useWorkspaceAiGatewayProvidersQuery({workspaceId});
    const {data: policiesData} = useWorkspaceAiGatewayRoutingPoliciesQuery({workspaceId});

    const providers = providersData?.workspaceAiGatewayProviders ?? [];
    const policies = policiesData?.workspaceAiGatewayRoutingPolicies ?? [];

    const isEditMode = !!model;

    const createMutation = useCreateWorkspaceAiModelMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiModels']});

            onClose();
        },
    });

    const updateMutation = useUpdateWorkspaceAiModelMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiModels']});

            onClose();
        },
    });

    const handleDefaultRoutingPolicyChange = useCallback((value: string) => {
        setDefaultRoutingPolicyId(value === INHERIT_ROUTING_POLICY_VALUE ? '' : value);
    }, []);

    const handleSubmit = useCallback(() => {
        // Null, not undefined: an omitted field leaves the stored routing policy in place, so
        // "Inherit from workspace/system default" has to be sent explicitly to clear it.
        const defaultRoutingPolicyIdInput = defaultRoutingPolicyId || null;

        if (isEditMode) {
            updateMutation.mutate({
                id: model.id,
                input: {
                    alias: alias || undefined,
                    capabilities: capabilities || undefined,
                    contextWindow: contextWindow ? parseInt(contextWindow, 10) : undefined,
                    defaultRoutingPolicyId: defaultRoutingPolicyIdInput,
                    inputCostPerMTokens: inputCostPerMTokens ? parseFloat(inputCostPerMTokens) : undefined,
                    name,
                    outputCostPerMTokens: outputCostPerMTokens ? parseFloat(outputCostPerMTokens) : undefined,
                },
                workspaceId,
            });
        } else {
            createMutation.mutate({
                input: {
                    alias: alias || undefined,
                    capabilities: capabilities || undefined,
                    contextWindow: contextWindow ? parseInt(contextWindow, 10) : undefined,
                    defaultRoutingPolicyId: defaultRoutingPolicyIdInput,
                    inputCostPerMTokens: inputCostPerMTokens ? parseFloat(inputCostPerMTokens) : undefined,
                    name,
                    outputCostPerMTokens: outputCostPerMTokens ? parseFloat(outputCostPerMTokens) : undefined,
                    providerId,
                    workspaceId,
                },
            });
        }
    }, [
        alias,
        capabilities,
        contextWindow,
        createMutation,
        defaultRoutingPolicyId,
        inputCostPerMTokens,
        isEditMode,
        model,
        name,
        outputCostPerMTokens,
        providerId,
        updateMutation,
        workspaceId,
    ]);

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
                    <DialogTitle>{isEditMode ? 'Edit Model' : 'Add Model'}</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayModelProvider">
                            Provider
                        </Label>

                        <Select disabled={isEditMode} onValueChange={setProviderId} value={providerId}>
                            <SelectTrigger id="aiGatewayModelProvider">
                                <SelectValue placeholder="Select a provider" />
                            </SelectTrigger>

                            <SelectContent>
                                {providers.filter(Boolean).map((provider) => (
                                    <SelectItem key={provider!.id} value={provider!.id}>
                                        {provider!.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayModelName">
                            Name
                        </Label>

                        <Input
                            id="aiGatewayModelName"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="gpt-4o"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayModelAlias">
                            Alias (optional)
                        </Label>

                        <Input
                            id="aiGatewayModelAlias"
                            onChange={(event) => setAlias(event.target.value)}
                            placeholder="my-gpt4"
                            value={alias}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayModelContextWindow">
                            Context Window (optional)
                        </Label>

                        <Input
                            id="aiGatewayModelContextWindow"
                            onChange={(event) => setContextWindow(event.target.value)}
                            placeholder="128000"
                            type="number"
                            value={contextWindow}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayModelInputCostPerMTokens">
                            Input Cost per 1M Tokens (optional)
                        </Label>

                        <Input
                            id="aiGatewayModelInputCostPerMTokens"
                            onChange={(event) => setInputCostPerMTokens(event.target.value)}
                            placeholder="2.50"
                            step="0.01"
                            type="number"
                            value={inputCostPerMTokens}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayModelOutputCostPerMTokens">
                            Output Cost per 1M Tokens (optional)
                        </Label>

                        <Input
                            id="aiGatewayModelOutputCostPerMTokens"
                            onChange={(event) => setOutputCostPerMTokens(event.target.value)}
                            placeholder="10.00"
                            step="0.01"
                            type="number"
                            value={outputCostPerMTokens}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayModelCapabilities">
                            Capabilities (optional)
                        </Label>

                        <Input
                            id="aiGatewayModelCapabilities"
                            onChange={(event) => setCapabilities(event.target.value)}
                            placeholder="CHAT,EMBEDDINGS"
                            value={capabilities}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="aiGatewayModelDefaultRoutingPolicy">
                            Default Routing Policy (optional)
                        </Label>

                        <Select
                            onValueChange={handleDefaultRoutingPolicyChange}
                            value={defaultRoutingPolicyId || INHERIT_ROUTING_POLICY_VALUE}
                        >
                            <SelectTrigger id="aiGatewayModelDefaultRoutingPolicy">
                                <SelectValue placeholder="Inherit from workspace/system default" />
                            </SelectTrigger>

                            <SelectContent>
                                <SelectItem value={INHERIT_ROUTING_POLICY_VALUE}>
                                    Inherit from workspace/system default
                                </SelectItem>

                                {policies.filter(Boolean).map((policy) => (
                                    <SelectItem key={policy!.id} value={policy!.id}>
                                        {policy!.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>
                </div>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={
                            !name ||
                            (!isEditMode && !providerId) ||
                            createMutation.isPending ||
                            updateMutation.isPending
                        }
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiGatewayModelDialog;
