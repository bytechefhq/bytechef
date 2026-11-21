import {Label} from '@/components/ui/label';
import AgentModelDialog, {AgentModelDialogValuesI} from '@/pages/automation/agents/components/detail/AgentModelDialog';
import AgentModelSummary from '@/pages/automation/agents/components/detail/AgentModelSummary';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiAgentElement,
    useAddAiAgentElementMutation,
    useDeleteAiAgentElementMutation,
    useUpdateAiAgentElementMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {toast} from 'sonner';

interface AgentModelCardProps {
    agentId: string;
    elements: AiAgentElement[];
}

const AgentModelCard = ({agentId, elements}: AgentModelCardProps) => {
    const modelElement = elements.find((element) => element.kind === 'MODEL');

    const [provider, setProvider] = useState<string>((modelElement?.parameters?.provider as string) ?? '');
    const [model, setModel] = useState<string>((modelElement?.parameters?.model as string) ?? '');
    const [connectionId, setConnectionId] = useState<string | null>(modelElement?.connectionId ?? null);
    const [showModelDialog, setShowModelDialog] = useState(false);

    const nestedParameters = (modelElement?.parameters?.parameters ?? {}) as Record<string, unknown>;

    const buildParameters = (
        next: {provider: string; model: string},
        nested: Record<string, unknown> = nestedParameters
    ) => ({
        model: next.model,
        parameters: nested,
        provider: next.provider,
    });

    const [elementId, setElementId] = useState<string | undefined>(modelElement?.id);
    const [committedConnectionId, setCommittedConnectionId] = useState<string | null>(
        modelElement?.connectionId ?? null
    );
    const [isSyncing, setIsSyncing] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const onError = (error: unknown) => {
        toast.error(error instanceof Error ? error.message : 'Failed to save the model.');

        setIsSyncing(false);
    };

    const addModelElementMutation = useAddAiAgentElementMutation({onError});
    const updateModelElementMutation = useUpdateAiAgentElementMutation({onError});
    const deleteModelElementMutation = useDeleteAiAgentElementMutation({onError});

    const isBusy =
        isSyncing ||
        addModelElementMutation.isPending ||
        updateModelElementMutation.isPending ||
        deleteModelElementMutation.isPending;

    const addElement = (
        next: {provider: string; model: string; connectionId: string | null},
        nested: Record<string, unknown> = nestedParameters
    ) => {
        addModelElementMutation.mutate(
            {
                input: {
                    agentId,
                    connectionId: next.connectionId,
                    kind: 'MODEL',
                    parameters: buildParameters(next, nested),
                },
            },
            {
                onSuccess: (data) => {
                    setElementId(data.addAiAgentElement.id);
                    setCommittedConnectionId(next.connectionId);
                    invalidateAgentQueries(queryClient);
                    setIsSyncing(false);
                },
            }
        );
    };

    const replaceElement = (
        next: {provider: string; model: string; connectionId: string | null},
        nested: Record<string, unknown> = nestedParameters
    ) => {
        if (!elementId) {
            return;
        }

        deleteModelElementMutation.mutate(
            {id: elementId},
            {
                onSuccess: () => addElement(next, nested),
            }
        );
    };

    const commit = (
        next: {provider: string; model: string; connectionId: string | null},
        nested: Record<string, unknown> = nestedParameters
    ) => {
        if (!next.provider || !next.model) {
            return;
        }

        setIsSyncing(true);

        if (!elementId) {
            addElement(next, nested);

            return;
        }

        const clearingConnection = next.connectionId === null && committedConnectionId != null;

        if (clearingConnection) {
            replaceElement(next, nested);

            return;
        }

        updateModelElementMutation.mutate(
            {
                input: {
                    connectionId: next.connectionId,
                    id: elementId,
                    parameters: buildParameters(next, nested),
                },
            },
            {
                onSuccess: () => {
                    setCommittedConnectionId(next.connectionId);
                    invalidateAgentQueries(queryClient);
                    setIsSyncing(false);
                },
            }
        );
    };

    const handleDialogSubmit = ({
        connectionId: nextConnectionId,
        model: nextModel,
        parameters: nextParameters,
        provider: nextProvider,
    }: AgentModelDialogValuesI) => {
        const providerChanged = nextProvider !== provider;

        setProvider(nextProvider);
        setModel(nextModel);
        setConnectionId(nextConnectionId);
        setShowModelDialog(false);

        if (!nextProvider || !nextModel) {
            return;
        }

        if (providerChanged && elementId) {
            setIsSyncing(true);

            replaceElement({connectionId: nextConnectionId, model: nextModel, provider: nextProvider}, nextParameters);

            return;
        }

        commit({connectionId: nextConnectionId, model: nextModel, provider: nextProvider}, nextParameters);
    };

    return (
        <AgentSection title="Model">
            <fieldset className="space-y-2 border-0 p-0">
                <Label htmlFor="agent-model-summary">LLM provider and model</Label>

                <AgentModelSummary
                    disabled={isBusy}
                    model={model}
                    onClick={() => setShowModelDialog(true)}
                    provider={provider}
                />
            </fieldset>

            {showModelDialog && currentWorkspaceId != null && (
                <AgentModelDialog
                    connectionId={connectionId}
                    model={model}
                    onClose={() => setShowModelDialog(false)}
                    onSubmit={handleDialogSubmit}
                    parameters={nestedParameters}
                    pending={isBusy}
                    provider={provider}
                    workspaceId={Number(currentWorkspaceId)}
                />
            )}
        </AgentSection>
    );
};

export default AgentModelCard;
