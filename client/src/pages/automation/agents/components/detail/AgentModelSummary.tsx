import {useAiProviderCatalogQuery} from '@/shared/middleware/graphql';
import {useGetRootComponentClusterElementDefinitions} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {BrainCircuitIcon, ChevronRightIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

interface AgentModelSummaryProps {
    disabled?: boolean;
    model: string;
    onClick: () => void;
    provider: string;
}

const MODEL_PROPERTY_NAME = 'model';

// Cluster element titles read "Antropic Model", "OpenAI Model", … — the trailing noun is redundant under a
// heading that already reads Model.
const stripModelSuffix = (title: string) => title.replace(/\s+Model$/, '');

/**
 * The Model section's single control: what the agent currently runs on, and the way in to change it.
 *
 * <p>
 * Owns the two lookups the label needs. The provider LIST is the aiAgent root's own `model` cluster elements
 * rather than the AI Gateway's catalog, which is gateway-scoped and would hide providers a non-gateway
 * workspace can still connect to. The catalog contributes display names only ("Claude Haiku 4.5" for
 * claude-haiku-4-5), keyed by the provider component's own name so the two line up without a mapping table.
 * </p>
 */
const AgentModelSummary = ({disabled, model, onClick, provider}: AgentModelSummaryProps) => {
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

    const selectedProvider = modelProviders.find((modelProvider) => modelProvider.componentName === provider);

    const catalogProvider = (catalogData?.aiProviderCatalog ?? []).find((item) => item.key === provider);

    const modelLabel = (catalogProvider?.models ?? []).find((catalogModel) => catalogModel.name === model)?.label;

    const label = model
        ? `${stripModelSuffix(selectedProvider?.title || provider)} · ${modelLabel || model}`
        : 'Select model';

    return (
        <button
            className="flex w-full items-center gap-2 rounded-md border border-input bg-background px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-50"
            disabled={disabled}
            // The Model section's <Label htmlFor> points here: the visible text is the current selection,
            // which says nothing about what is being chosen, so the button needs the label to name it.
            id="agent-model-summary"
            onClick={onClick}
            type="button"
        >
            {/* The chosen provider's own logo once one is picked, so the row matches the provider list it was
                chosen from; the generic brain stands in only while nothing is selected. */}

            {selectedProvider?.icon ? (
                <InlineSVG aria-hidden className="size-4 shrink-0" src={selectedProvider.icon} />
            ) : (
                <BrainCircuitIcon aria-hidden className="size-4 shrink-0 text-content-neutral-tertiary" />
            )}

            <span className="flex-1 truncate text-left font-medium">{label}</span>

            {/* A chevron pointing on rather than down: this opens a dialog, not a menu below it. */}

            <ChevronRightIcon aria-hidden className="size-4 shrink-0 text-content-neutral-tertiary" />
        </button>
    );
};

export default AgentModelSummary;
