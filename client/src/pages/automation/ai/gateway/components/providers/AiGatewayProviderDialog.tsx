import Button from '@/components/Button/Button';
import {
    AiGatewayProviderType as AiGatewayProviderTypeEnum,
    useCreateWorkspaceAiGatewayProviderMutation,
    useTestWorkspaceAiGatewayProviderConnectionMutation,
    useUpdateWorkspaceAiGatewayProviderMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {useCallback, useState} from 'react';
import {toast} from 'sonner';

import {AiGatewayProviderType} from '../../types';

interface AiGatewayProviderDialogProps {
    onClose: () => void;
    provider?: AiGatewayProviderType;
    workspaceId: string;
}

const PROVIDER_TYPES: AiGatewayProviderTypeEnum[] = [
    AiGatewayProviderTypeEnum.Anthropic,
    AiGatewayProviderTypeEnum.AzureOpenai,
    AiGatewayProviderTypeEnum.Cohere,
    AiGatewayProviderTypeEnum.Deepseek,
    AiGatewayProviderTypeEnum.GoogleGemini,
    AiGatewayProviderTypeEnum.Groq,
    AiGatewayProviderTypeEnum.Mistral,
    AiGatewayProviderTypeEnum.Openai,
];

const AiGatewayProviderDialog = ({onClose, provider, workspaceId}: AiGatewayProviderDialogProps) => {
    const [apiKey, setApiKey] = useState('');
    const [baseUrl, setBaseUrl] = useState(provider?.baseUrl ?? '');
    const [name, setName] = useState(provider?.name ?? '');
    const [type, setType] = useState<AiGatewayProviderTypeEnum>(provider?.type ?? AiGatewayProviderTypeEnum.Openai);

    const queryClient = useQueryClient();

    const isEditMode = !!provider;

    const createMutation = useCreateWorkspaceAiGatewayProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiGatewayProviders']});

            onClose();
        },
    });

    const updateMutation = useUpdateWorkspaceAiGatewayProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['workspaceAiGatewayProviders']});

            onClose();
        },
    });

    const testConnectionMutation = useTestWorkspaceAiGatewayProviderConnectionMutation({
        onError: (error: Error) => toast.error(`Connection test failed: ${error.message}`),
        onSuccess: (data) => {
            const result = data.testWorkspaceAiGatewayProviderConnection;

            if (!result) {
                toast.error('Connection test returned no result');

                return;
            }

            if (result.ok) {
                toast.success(`Connected in ${result.latencyMs}ms`);
            } else {
                toast.error(result.errorMessage || 'Connection failed');
            }
        },
    });

    const handleTestConnection = useCallback(() => {
        if (provider?.id) {
            testConnectionMutation.mutate({providerId: provider.id, workspaceId});
        }
    }, [provider, testConnectionMutation, workspaceId]);

    const handleSubmit = useCallback(() => {
        if (isEditMode) {
            updateMutation.mutate({
                id: provider.id,
                input: {
                    apiKey: apiKey || undefined,
                    baseUrl: baseUrl || undefined,
                    name,
                    type,
                },
                workspaceId,
            });
        } else {
            createMutation.mutate({
                input: {
                    apiKey,
                    baseUrl: baseUrl || undefined,
                    name,
                    type,
                    workspaceId,
                },
            });
        }
    }, [apiKey, baseUrl, createMutation, isEditMode, name, provider, type, updateMutation, workspaceId]);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-lg font-medium">{isEditMode ? 'Edit Provider' : 'Add Provider'}</h3>

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
                            placeholder="My OpenAI Provider"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Type</label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setType(event.target.value as AiGatewayProviderTypeEnum)}
                            value={type}
                        >
                            {PROVIDER_TYPES.map((providerType) => (
                                <option key={providerType} value={providerType}>
                                    {providerType}
                                </option>
                            ))}
                        </select>
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">API Key</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setApiKey(event.target.value)}
                            placeholder={isEditMode ? 'Leave empty to keep current' : 'sk-...'}
                            type="password"
                            value={apiKey}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <label className="mb-1 block text-sm font-medium">Base URL (optional)</label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            onChange={(event) => setBaseUrl(event.target.value)}
                            placeholder="https://api.openai.com"
                            value={baseUrl}
                        />
                    </fieldset>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    {isEditMode && (
                        <Button
                            disabled={testConnectionMutation.isPending}
                            label={testConnectionMutation.isPending ? 'Testing...' : 'Test Connection'}
                            onClick={handleTestConnection}
                            variant="outline"
                        />
                    )}

                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={
                            !name || (!isEditMode && !apiKey) || createMutation.isPending || updateMutation.isPending
                        }
                        label={isEditMode ? 'Save' : 'Create'}
                        onClick={handleSubmit}
                    />
                </div>
            </div>
        </div>
    );
};

export default AiGatewayProviderDialog;
