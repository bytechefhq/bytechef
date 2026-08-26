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

    // The element's parameters map is {provider, model, parameters: {...the model node's own properties}}.
    // Every writer has to rebuild the whole map, so it is built in one place -- writing {model, provider}
    // alone (as this did) silently dropped temperature, maxTokens and everything else the config dialog sets.
    // `model` is kept authoritative at the top level: AiAgentWorkflowGenerator.buildModelElement writes it
    // LAST precisely so a stale copy inside the nested map cannot shadow it.
    const buildParameters = (
        next: {provider: string; model: string},
        nested: Record<string, unknown> = nestedParameters
    ) => ({
        model: next.model,
        parameters: nested,
        provider: next.provider,
    });

    // Local mirror of what the server currently has, updated only from mutation results — never
    // resynced from the `elements` prop (the parent's post-mutation refetch lands asynchronously, well
    // after the mutation itself settles, so relying on it would leave a window where a second edit reads
    // stale data; the AgentDetail page keys this card's subtree by agent id instead, so a fresh agent
    // gets a fresh instance rather than stale local state).
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

    // updateAiAgentElement treats a null connectionId as "leave unchanged" (AiAgentFacadeImpl), so it can
    // never clear a wired connection — replacing the row is the only way to actually detach it. Also
    // used for a provider switch: the old connectionId belongs to the old provider's component, so
    // carrying it over via update would silently leave a foreign-provider connection wired to the new
    // model row.
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

    // Requires both fields before persisting anything: creating a MODEL row from a provider pick alone
    // (before a model name exists) would let a later model-name blur race the still-in-flight create and
    // hit ELEMENT_KIND_ALREADY_PRESENT server-side (a second add), losing the typed model name.
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

    // The dialog gathers provider, model, connection and properties behind one Save, so a single submit is
    // always a complete configuration — unlike the picker this replaced, which committed on every pick and
    // had to deal with half-configured states between them.
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

        // A provider change always replaces rather than updates once a row exists, so neither the old
        // connectionId NOR the old provider's properties survive onto the new provider's model row --
        // temperature and friends are that provider's model element's own properties, not portable ones. The
        // dialog has already blanked both in what it hands back.
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

                {/* One control rather than the dropdown-plus-gear pair this replaced: provider, model,
                    connection and properties are four parts of one decision, and splitting them across two
                    affordances meant a provider switch silently discarded what the gear had configured. */}

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
