import ComboBox from '@/components/ComboBox/ComboBox';
import {Input} from '@/components/ui/input';
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

const EXCLUDED_PROPERTY_NAMES = [MODEL_PROPERTY_NAME];

const EMPTY_INITIAL_VALUES: ComponentConfigDialogInitialValuesI = {connectionId: null, parameters: {}};

const stripModelSuffix = (title: string) => title.replace(/\s+Model$/, '');

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
                label: catalogLabels[option.value!] || option.label || option.value!,
                value: option.value!,
            }));
    }, [catalogData, clusterElementDefinition, draftProvider]);

    const modelEntered = !!clusterElementDefinition && modelItems.length === 0;

    const target = useMemo(
        () =>
            draftProvider && draftModel
                ? {
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

                        {modelEntered ? (
                            <Input
                                aria-label="Model"
                                id="agent-model-name"
                                name="agentModelName"
                                onChange={(event) => setDraftModel(event.target.value.trim())}
                                placeholder="Model ID, for example llama3.1"
                                value={draftModel}
                            />
                        ) : (
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
                        )}
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
