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

const stripModelSuffix = (title: string) => title.replace(/\s+Model$/, '');

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
            id="agent-model-summary"
            onClick={onClick}
            type="button"
        >
            {selectedProvider?.icon ? (
                <InlineSVG aria-hidden className="size-4 shrink-0" src={selectedProvider.icon} />
            ) : (
                <BrainCircuitIcon aria-hidden className="size-4 shrink-0 text-content-neutral-tertiary" />
            )}

            <span className="flex-1 truncate text-left font-medium">{label}</span>

            <ChevronRightIcon aria-hidden className="size-4 shrink-0 text-content-neutral-tertiary" />
        </button>
    );
};

export default AgentModelSummary;
