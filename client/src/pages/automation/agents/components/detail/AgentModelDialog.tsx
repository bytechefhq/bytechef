import ComboBox from '@/components/ComboBox/ComboBox';
import {Label} from '@/components/ui/label';
import ComponentConfigDialog, {
    ComponentConfigDialogInitialValuesI,
} from '@/shared/components/component-config/ComponentConfigDialog';
import {useAiProviderCatalogQuery} from '@/shared/middleware/graphql';
import {
    useGetClusterElementDefinitionQuery,
    useGetRootComponentClusterElementDefinitions,
} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo, useState} from 'react';

export interface AgentModelDialogValuesI {
    connectionId: string | null;
    model: string;
    parameters: Record<string, unknown>;
    provider: string;
}

interface AgentModelDialogProps {
    connectionId: string | null;
    model: string;
    onClose: () => void;
    onSubmit: (values: AgentModelDialogValuesI) => void;
    parameters: Record<string, unknown>;
    pending?: boolean;
    provider: string;
    workspaceId: number;
}

const MODEL_PROPERTY_NAME = 'model';

// The model cluster element declares `model` among its OWN properties, so the Properties tab would render a
// second Model field beside the combobox above — one showing the schema default (gpt-3.5-turbo-0125) rather
// than the chosen model, and writing that default back into the nested parameters map on save. The combobox
// owns this field; hoisted so its identity never churns the dialog's property memos.
const EXCLUDED_PROPERTY_NAMES = [MODEL_PROPERTY_NAME];

// A provider switch leaves nothing to carry over: the connection belongs to the old provider's component and
// the properties are that provider's model element's own. Hoisted rather than built inline because the shared
// dialog re-seeds its form whenever this object's identity changes.
const EMPTY_INITIAL_VALUES: ComponentConfigDialogInitialValuesI = {connectionId: null, parameters: {}};

// Cluster element titles read "Antropic Model", "OpenAI Model", … — the trailing noun is redundant beside a
// field already labelled Provider, inside a dialog already titled Model.
const stripModelSuffix = (title: string) => title.replace(/\s+Model$/, '');

/**
 * Chooses the agent's LLM: provider and model as the dialog's picker, then the connection it authenticates
 * with and the model element's own properties as the two tabs below — the same shape the Add Tool dialog uses
 * for component and tool.
 *
 * <p>
 * Deliberately NOT fed by the AI Gateway's provider catalog (`aiProviderCatalog`): an agent's MODEL cluster
 * element runs on an ordinary provider connection, so a workspace not using the gateway would be shown the
 * wrong set of providers. The list is the aiAgent root component's own `model` cluster elements; the catalog
 * contributes display names only, and its `key` is the provider component's own name, so the two line up
 * without a mapping table.
 * </p>
 *
 * <p>
 * Nothing is persisted until Save. That matters because a provider change cannot be an update server-side —
 * it replaces the whole element — so gathering all four decisions behind one submit turns what used to be a
 * chain of eager mutations into a single commit the user can still cancel out of.
 * </p>
 */
const AgentModelDialog = ({
    connectionId,
    model,
    onClose,
    onSubmit,
    parameters,
    pending,
    provider,
    workspaceId,
}: AgentModelDialogProps) => {
    const [draftProvider, setDraftProvider] = useState(provider);
    const [draftModel, setDraftModel] = useState(model);

    // Snapshot of what the agent already has, taken once. Read back only while the draft still points at the
    // provider it was captured from.
    const [committedValues] = useState<ComponentConfigDialogInitialValuesI>(() => ({connectionId, parameters}));

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: modelProviders = []} = useGetRootComponentClusterElementDefinitions({
        clusterElementType: MODEL_PROPERTY_NAME,
        rootComponentName: 'aiAgent',
        rootComponentVersion: 1,
    });

    const {data: catalogData} = useAiProviderCatalogQuery(
        {environment: String(currentEnvironmentId ?? 0)},
        {enabled: currentEnvironmentId != null}
    );

    const draftProviderDefinition = modelProviders.find(
        (modelProvider) => modelProvider.componentName === draftProvider
    );

    // A provider's models come from its model cluster element's own `model` property, whose options are a
    // static list baked into the component definition (e.g. AnthropicConstants.MODELS) — there is no
    // options-function round trip to make, so the definition query is the whole source.
    const {data: clusterElementDefinition} = useGetClusterElementDefinitionQuery(
        {
            clusterElementName: MODEL_PROPERTY_NAME,
            clusterElementType: MODEL_PROPERTY_NAME,
            componentName: draftProvider,
            componentVersion: draftProviderDefinition?.componentVersion ?? 1,
        },
        !!draftProvider
    );

    const providerItems = useMemo(
        () =>
            modelProviders.map((modelProvider) => ({
                icon: modelProvider.icon,
                label: stripModelSuffix(modelProvider.title || modelProvider.componentName),
                value: modelProvider.componentName,
            })),
        [modelProviders]
    );

    const modelItems = useMemo(() => {
        const catalogProvider = (catalogData?.aiProviderCatalog ?? []).find((item) => item.key === draftProvider);

        const catalogLabels = Object.fromEntries(
            (catalogProvider?.models ?? []).map((catalogModel) => [catalogModel.name, catalogModel.label])
        );

        const modelProperty = (clusterElementDefinition?.properties ?? []).find(
            (property) => property.name === MODEL_PROPERTY_NAME
        );

        const options = (modelProperty as {options?: {label?: string; value?: string}[]} | undefined)?.options ?? [];

        return options
            .filter((option) => option.value)
            .map((option) => ({
                // Label precedence mirrors AiProviderFacadeImpl's: models.dev display name → the component's
                // own option label → the raw model id, never blank.
                label: catalogLabels[option.value!] || option.label || option.value!,
                value: option.value!,
            }));
    }, [catalogData, clusterElementDefinition, draftProvider]);

    // Both tabs are drawn from the target's definition, and a model id only means anything alongside the
    // provider that serves it, so neither opens until both halves are chosen.
    const target = useMemo(
        () =>
            draftProvider && draftModel
                ? {
                      // Every LLM provider names its model cluster element "model" (MODEL_ELEMENT_NAME in the
                      // generator), so the provider IS the component and the element name is fixed.
                      clusterElementName: MODEL_PROPERTY_NAME,
                      componentName: draftProvider,
                      componentVersion: draftProviderDefinition?.componentVersion ?? 1,
                      title: stripModelSuffix(draftProviderDefinition?.title || draftProvider),
                  }
                : null,
        [draftModel, draftProvider, draftProviderDefinition]
    );

    const handleProviderChange = (value: string) => {
        if (value === draftProvider) {
            return;
        }

        setDraftProvider(value);

        // Back on the provider the agent already runs, its committed model is the right draft again;
        // anywhere else there is nothing to inherit.
        setDraftModel(value === provider ? model : '');
    };

    return (
        <ComponentConfigDialog
            description="Choose the provider and model this agent runs on, then set the connection and properties it uses."
            excludedPropertyNames={EXCLUDED_PROPERTY_NAMES}
            initialValues={draftProvider === provider ? committedValues : EMPTY_INITIAL_VALUES}
            onClose={onClose}
            onSubmit={({connectionId: nextConnectionId, parameters: nextParameters}) =>
                onSubmit({
                    connectionId: nextConnectionId,
                    model: draftModel,
                    parameters: nextParameters,
                    provider: draftProvider,
                })
            }
            open
            pending={pending}
            picker={
                <>
                    <div className="space-y-1">
                        <Label htmlFor="agent-model-provider">Provider</Label>

                        <ComboBox
                            ariaLabel="Provider"
                            items={providerItems}
                            maxHeight
                            name="agentModelProvider"
                            onChange={(item) => handleProviderChange(String(item?.value ?? ''))}
                            value={draftProvider}
                        />
                    </div>

                    <div className="space-y-1">
                        <Label htmlFor="agent-model-name">Model</Label>

                        <ComboBox
                            ariaLabel="Model"
                            disabled={!draftProvider}
                            emptyMessage="No models listed for this provider."
                            items={modelItems}
                            maxHeight
                            name="agentModelName"
                            onChange={(item) => setDraftModel(String(item?.value ?? ''))}
                            value={draftModel}
                        />
                    </div>
                </>
            }
            target={target}
            title="Model"
            workspaceId={workspaceId}
        />
    );
};

export default AgentModelDialog;
