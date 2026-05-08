import Button from '@/components/Button/Button';
import {
    useCreateWorkspaceAiGatewayModelMutation,
    useUpdateWorkspaceAiGatewayModelMutation,
    useWorkspaceAiGatewayProvidersQuery,
    useWorkspaceAiGatewayRoutingPoliciesQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useState} from 'react';

import {AiGatewayModelType} from '../../types';

interface AiGatewayModelDialogProps {
    model?: AiGatewayModelType;
    onClose: () => void;
    workspaceId: string;
}

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

    const createMutation = useCreateWorkspaceAiGatewayModelMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiGatewayModels']});

            onClose();
        },
    });

    const updateMutation = useUpdateWorkspaceAiGatewayModelMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiGatewayModels']});

            onClose();
        },
    });

    const handleSubmit = useCallback(() => {
        if (isEditMode) {
            updateMutation.mutate({
                id: model.id,
                input: {
                    alias: alias || undefined,
                    capabilities: capabilities || undefined,
                    contextWindow: contextWindow ? parseInt(contextWindow, 10) : undefined,
                    defaultRoutingPolicyId: defaultRoutingPolicyId || undefined,
                    inputCostPerMTokens: inputCostPerMTokens ? parseFloat(inputCostPerMTokens) : undefined,
                    name,
                    outputCostPerMTokens: outputCostPerMTokens ? parseFloat(outputCostPerMTokens) : undefined,
                },
            });
        } else {
            createMutation.mutate({
                input: {
                    alias: alias || undefined,
                    capabilities: capabilities || undefined,
                    contextWindow: contextWindow ? parseInt(contextWindow, 10) : undefined,
                    defaultRoutingPolicyId: defaultRoutingPolicyId || undefined,
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">{isEditMode ? 'Edit Model' : 'Add Model'}</h3>

                    <button onClick={onClose}>
                        <XIcon className="size-4" />
                    </button>
                </div>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Provider</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            disabled={isEditMode}
                            onChange={(event) => setProviderId(event.target.value)}
                            value={providerId}
                        >
                            <option value="">Select a provider</option>

                            {providers.filter(Boolean).map((provider) => (
                                <option key={provider!.id} value={provider!.id}>
                                    {provider!.name}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Name</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="gpt-4o"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Alias (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setAlias(event.target.value)}
                            placeholder="my-gpt4"
                            value={alias}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Context Window (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setContextWindow(event.target.value)}
                            placeholder="128000"
                            type="number"
                            value={contextWindow}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Input Cost per 1M Tokens (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setInputCostPerMTokens(event.target.value)}
                            placeholder="2.50"
                            step="0.01"
                            type="number"
                            value={inputCostPerMTokens}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Output Cost per 1M Tokens (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setOutputCostPerMTokens(event.target.value)}
                            placeholder="10.00"
                            step="0.01"
                            type="number"
                            value={outputCostPerMTokens}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Capabilities (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setCapabilities(event.target.value)}
                            placeholder="CHAT,EMBEDDINGS"
                            value={capabilities}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Default Routing Policy (optional)</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setDefaultRoutingPolicyId(event.target.value)}
                            value={String(defaultRoutingPolicyId ?? '')}
                        >
                            <option value="">Inherit from workspace/system default</option>

                            {policies.filter(Boolean).map((policy) => (
                                <option key={policy!.id} value={policy!.id}>
                                    {policy!.name}
                                </option>
                            ))}
                        </select>
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
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
                </div>
            </div>
        </div>
    );
};

export default AiGatewayModelDialog;
