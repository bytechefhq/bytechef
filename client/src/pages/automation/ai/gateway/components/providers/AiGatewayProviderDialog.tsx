import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {
    AiGatewayProviderType as AiGatewayProviderTypeEnum,
    useCreateWorkspaceAiGatewayProviderMutation,
    useTestWorkspaceAiGatewayProviderConnectionMutation,
    useUpdateWorkspaceAiGatewayProviderMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
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
                    <DialogTitle>{isEditMode ? 'Edit Provider' : 'Add Provider'}</DialogTitle>
                </DialogHeader>

                <div className="space-y-4">
                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-gateway-provider-name">
                            Name
                        </Label>

                        <Input
                            id="ai-gateway-provider-name"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="My OpenAI Provider"
                            value={name}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-gateway-provider-type">
                            Type
                        </Label>

                        <Select onValueChange={(value) => setType(value as AiGatewayProviderTypeEnum)} value={type}>
                            <SelectTrigger id="ai-gateway-provider-type">
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {PROVIDER_TYPES.map((providerType) => (
                                    <SelectItem key={providerType} value={providerType}>
                                        {providerType}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-gateway-provider-api-key">
                            API Key
                        </Label>

                        <Input
                            id="ai-gateway-provider-api-key"
                            onChange={(event) => setApiKey(event.target.value)}
                            placeholder={isEditMode ? 'Leave empty to keep current' : 'sk-...'}
                            type="password"
                            value={apiKey}
                        />
                    </fieldset>

                    <fieldset className="border-0">
                        <Label className="mb-1 block" htmlFor="ai-gateway-provider-base-url">
                            Base URL (optional)
                        </Label>

                        <Input
                            id="ai-gateway-provider-base-url"
                            onChange={(event) => setBaseUrl(event.target.value)}
                            placeholder="https://api.openai.com"
                            value={baseUrl}
                        />
                    </fieldset>
                </div>

                <DialogFooter>
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
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiGatewayProviderDialog;
