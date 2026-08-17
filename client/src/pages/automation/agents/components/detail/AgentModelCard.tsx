import Button from '@/components/Button/Button';
import {Label} from '@/components/ui/label';
import AgentModelPicker, {AgentModelSelectionI} from '@/pages/automation/agents/components/detail/AgentModelPicker';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ComponentConfigDialog from '@/shared/components/component-config/ComponentConfigDialog';
import {
    AiAgentElement,
    useAddAiAgentElementMutation,
    useDeleteAiAgentElementMutation,
    useUpdateAiAgentElementMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {SettingsIcon} from 'lucide-react';
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
    const [showConfigDialog, setShowConfigDialog] = useState(false);

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

    // The picker reports provider and model together, so unlike the old provider-Select + model-Input pair
    // there is no half-configured state to defer around — a pick always carries both.
    const handlePickerChange = (selection: AgentModelSelectionI) => {
        const providerChanged = selection.provider !== provider;

        setProvider(selection.provider);
        setModel(selection.model);

        if (providerChanged) {
            setConnectionId(null);
        }

        if (!selection.provider || !selection.model) {
            return;
        }

        // A provider change always replaces rather than updates once a row exists, so neither the old
        // connectionId NOR the old provider's properties survive onto the new provider's model row --
        // temperature and friends are that provider's model element's own properties, not portable ones.
        if (providerChanged && elementId) {
            setIsSyncing(true);
            replaceElement({...selection, connectionId: null}, {});

            return;
        }

        commit({...selection, connectionId: providerChanged ? null : connectionId});
    };

    return (
        <AgentSection title="Model">
            <fieldset className="space-y-2 border-0 p-0">
                <Label htmlFor="agent-model-picker">LLM provider and model</Label>

                <div className="flex items-center gap-2">
                    <div className="flex-1">
                        <AgentModelPicker
                            disabled={isBusy}
                            model={model}
                            onChange={handlePickerChange}
                            provider={provider}
                        />
                    </div>

                    {/* Connection and the model's own properties (temperature, maxTokens, …) are edited in the
                        config dialog, the same one a tool row opens. The provider is deliberately NOT editable
                        there: changing it replaces the element rather than updating it, so it stays with the
                        picker above, which already owns that path. */}

                    <Button
                        aria-label="Configure model"
                        disabled={isBusy || !provider}
                        icon={<SettingsIcon />}
                        onClick={() => setShowConfigDialog(true)}
                        size="icon"
                        variant="outline"
                    />
                </div>
            </fieldset>

            {showConfigDialog && currentWorkspaceId != null && provider && (
                <ComponentConfigDialog
                    initialValues={{connectionId, parameters: nestedParameters}}
                    onClose={() => setShowConfigDialog(false)}
                    onSubmit={({connectionId: nextConnectionId, parameters}) => {
                        // Routed through commit rather than a direct mutation so the connection-clearing case
                        // still replaces the row: updateAiAgentElement reads a null connectionId as "leave
                        // unchanged", so clearing one cannot go through an update.
                        setConnectionId(nextConnectionId);

                        commit({connectionId: nextConnectionId, model, provider}, parameters);

                        setShowConfigDialog(false);
                    }}
                    open
                    pending={isBusy}
                    target={{
                        // Every LLM provider names its model cluster element "model" (MODEL_ELEMENT_NAME in
                        // the generator), so the provider IS the component and the element name is fixed.
                        clusterElementName: 'model',
                        componentName: provider,
                        componentVersion: 1,
                    }}
                    workspaceId={Number(currentWorkspaceId)}
                />
            )}
        </AgentSection>
    );
};

export default AgentModelCard;
