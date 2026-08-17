import {Input} from '@/components/Input/Input';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuPortal,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useAiProviderCatalogQuery} from '@/shared/middleware/graphql';
import {
    useGetClusterElementDefinitionQuery,
    useGetRootComponentClusterElementDefinitions,
} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {BrainCircuitIcon, CheckIcon, ChevronDownIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

export interface AgentModelSelectionI {
    model: string;
    provider: string;
}

interface AgentModelPickerProps {
    disabled?: boolean;
    model: string;
    onChange: (selection: AgentModelSelectionI) => void;
    provider: string;
}

interface AgentModelProviderSubMenuProps {
    componentName: string;
    componentVersion: number;
    model: string;
    modelLabels: Record<string, string>;
    onChange: (selection: AgentModelSelectionI) => void;
    provider: string;
    title: string;
}

const MODEL_PROPERTY_NAME = 'model';

// Cluster element titles read "Antropic Model", "OpenAI Model", … — the trailing noun is redundant once the
// control is already labelled "LLM provider and model".
const stripModelSuffix = (title: string) => title.replace(/\s+Model$/, '');

/**
 * A provider's models, resolved only once its submenu is opened. They come from the model cluster element's
 * own `model` property, whose options are a static list baked into the component definition (e.g.
 * AnthropicConstants.MODELS) — there is no options-function round trip to make, so the definition query is
 * the whole source.
 */
const AgentModelProviderSubMenu = ({
    componentName,
    componentVersion,
    model,
    modelLabels,
    onChange,
    provider,
    title,
}: AgentModelProviderSubMenuProps) => {
    const {data: clusterElementDefinition} = useGetClusterElementDefinitionQuery({
        clusterElementName: MODEL_PROPERTY_NAME,
        clusterElementType: MODEL_PROPERTY_NAME,
        componentName,
        componentVersion,
    });

    const models = useMemo(() => {
        const modelProperty = (clusterElementDefinition?.properties ?? []).find(
            (property) => property.name === MODEL_PROPERTY_NAME
        );

        return ((modelProperty as {options?: {label?: string; value?: string}[]} | undefined)?.options ?? []).filter(
            (option) => option.value
        );
    }, [clusterElementDefinition]);

    const isCurrentProvider = provider === componentName;

    return (
        <DropdownMenuSubContent className="max-h-96 overflow-y-auto">
            {models.length ? (
                models.map((option) => {
                    // Label precedence mirrors AiProviderFacadeImpl's: models.dev display name → the
                    // component's own option label → the raw model id, never blank.
                    const catalogLabel = modelLabels[option.value!];
                    const label = catalogLabel || option.label || option.value;

                    return (
                        <DropdownMenuItem
                            key={option.value}
                            onClick={() => onChange({model: option.value!, provider: componentName})}
                        >
                            <div className="flex flex-1 flex-col">
                                <span>{label}</span>

                                {label !== option.value && (
                                    <span className="text-xs text-muted-foreground">{option.value}</span>
                                )}
                            </div>

                            {isCurrentProvider && model === option.value && <CheckIcon className="size-4" />}
                        </DropdownMenuItem>
                    );
                })
            ) : (
                <span className="px-2 py-1.5 text-xs text-muted-foreground">No models listed for {title}.</span>
            )}
        </DropdownMenuSubContent>
    );
};

/**
 * A searchable provider list with each provider's models behind it, for choosing the agent's LLM.
 *
 * <p>
 * Deliberately NOT the shared {@code ModelPicker}: that one reads the AI Gateway's provider catalog
 * (`aiProviderCatalog`), while an agent's MODEL cluster element runs on an ordinary provider connection —
 * so a workspace not using the gateway would see the wrong set of providers. The list here is the aiAgent
 * root component's own `model` cluster elements. Only the shape is borrowed.
 * </p>
 */
const AgentModelPicker = ({disabled, model, onChange, provider}: AgentModelPickerProps) => {
    const [open, setOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: modelProviders = []} = useGetRootComponentClusterElementDefinitions({
        clusterElementType: MODEL_PROPERTY_NAME,
        rootComponentName: 'aiAgent',
        rootComponentVersion: 1,
    });

    // Labels only. The provider LIST stays the cluster elements' — the catalog is gateway-scoped and would
    // hide providers a non-gateway workspace can still connect to — but its models.dev display names
    // ("Claude Haiku 4.5" for claude-haiku-4-5) are worth borrowing where they exist. Its `key` is the
    // provider component's own name (Provider.getKey()), so the two line up without a mapping table.
    const {data: catalogData} = useAiProviderCatalogQuery(
        {environment: String(currentEnvironmentId ?? 0)},
        {enabled: currentEnvironmentId != null}
    );

    const modelLabelsByProvider = useMemo(() => {
        const labels: Record<string, Record<string, string>> = {};

        for (const item of catalogData?.aiProviderCatalog ?? []) {
            labels[item.key] = Object.fromEntries(
                (item.models ?? []).map((catalogModel) => [catalogModel.name, catalogModel.label])
            );
        }

        return labels;
    }, [catalogData]);

    const filteredProviders = useMemo(() => {
        const query = searchQuery.trim().toLowerCase();

        if (!query) {
            return modelProviders;
        }

        return modelProviders.filter((modelProvider) =>
            (modelProvider.title || modelProvider.componentName).toLowerCase().includes(query)
        );
    }, [modelProviders, searchQuery]);

    const selectedProvider = modelProviders.find((modelProvider) => modelProvider.componentName === provider);

    const selectedModelLabel = modelLabelsByProvider[provider]?.[model];

    const triggerLabel = model
        ? `${stripModelSuffix(selectedProvider?.title || provider)} · ${selectedModelLabel || model}`
        : 'Select model';

    const handleChange = (selection: AgentModelSelectionI) => {
        setOpen(false);

        onChange(selection);
    };

    return (
        <DropdownMenu onOpenChange={setOpen} open={open}>
            <DropdownMenuTrigger asChild disabled={disabled}>
                <button
                    className="flex w-full items-center gap-2 rounded-md border border-input bg-background px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-50"
                    type="button"
                >
                    {/* The chosen provider's own logo once one is picked, so the trigger matches the submenu
                        rows it was chosen from; the generic brain stands in only while nothing is selected. */}

                    {selectedProvider?.icon ? (
                        <InlineSVG aria-hidden className="size-4 shrink-0" src={selectedProvider.icon} />
                    ) : (
                        <BrainCircuitIcon aria-hidden className="size-4 shrink-0 text-content-neutral-tertiary" />
                    )}

                    {/* Left-aligned, like every other select-shaped control in the app — centring left the
                        label drifting between the icon and the chevron as the model name changed length. */}

                    <span className="flex-1 truncate text-left font-medium">{triggerLabel}</span>

                    <ChevronDownIcon aria-hidden className="size-4 shrink-0 text-content-neutral-tertiary" />
                </button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="start" className="w-72">
                <div className="p-1">
                    <Input
                        onChange={(event) => setSearchQuery(event.target.value)}
                        placeholder="Search providers..."
                        value={searchQuery}
                    />
                </div>

                {filteredProviders.map((modelProvider) => (
                    <DropdownMenuSub key={modelProvider.componentName}>
                        <DropdownMenuSubTrigger>
                            {modelProvider.icon ? (
                                <InlineSVG className="mr-2 size-4" src={modelProvider.icon} />
                            ) : (
                                <BrainCircuitIcon className="mr-2 size-4" />
                            )}

                            <span className="flex-1">
                                {stripModelSuffix(modelProvider.title || modelProvider.componentName)}
                            </span>
                        </DropdownMenuSubTrigger>

                        <DropdownMenuPortal>
                            <AgentModelProviderSubMenu
                                componentName={modelProvider.componentName}
                                componentVersion={modelProvider.componentVersion}
                                model={model}
                                modelLabels={modelLabelsByProvider[modelProvider.componentName] ?? {}}
                                onChange={handleChange}
                                provider={provider}
                                title={stripModelSuffix(modelProvider.title || modelProvider.componentName)}
                            />
                        </DropdownMenuPortal>
                    </DropdownMenuSub>
                ))}

                {!filteredProviders.length && (
                    <span className="px-2 py-1.5 text-xs text-muted-foreground">No providers match.</span>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default AgentModelPicker;
