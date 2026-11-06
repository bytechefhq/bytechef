import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiDatasetItemsQuery,
    AiDatasetVersionsQuery,
    AiDatasetsQuery,
    useAiDatasetItemsQuery,
    useAiDatasetVersionsQuery,
    useAiDatasetsQuery,
} from '@/shared/middleware/graphql';
import {ChevronLeftIcon, DatabaseIcon, LockIcon, UnlockIcon} from 'lucide-react';
import {useState} from 'react';

type DatasetType = AiDatasetsQuery['aiDatasets'][number];
type DatasetVersionType = AiDatasetVersionsQuery['aiDatasetVersions'][number];
type DatasetItemType = AiDatasetItemsQuery['aiDatasetItems'][number];

const formatTimestamp = (epochMs: number | null | undefined): string => {
    if (epochMs == null) {
        return '—';
    }

    return new Date(epochMs).toLocaleString();
};

const truncate = (text: string | null | undefined, max = 200): string => {
    if (text == null) {
        return '—';
    }

    if (text.length <= max) {
        return text;
    }

    return `${text.slice(0, max - 1)}…`;
};

const AiDatasets = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [selectedDataset, setSelectedDataset] = useState<DatasetType | null>(null);
    const [selectedVersion, setSelectedVersion] = useState<DatasetVersionType | null>(null);

    const workspaceIdString = currentWorkspaceId != null ? String(currentWorkspaceId) : '';

    const {
        data: datasetsData,
        error: datasetsError,
        isLoading: datasetsIsLoading,
    } = useAiDatasetsQuery({workspaceId: workspaceIdString}, {enabled: workspaceIdString.length > 0});

    const datasets = (datasetsData?.aiDatasets ?? [])
        .slice()
        .sort((left, right) => (right.createdDate ?? 0) - (left.createdDate ?? 0));

    if (selectedVersion != null) {
        return <DatasetVersionItems onBack={() => setSelectedVersion(null)} version={selectedVersion} />;
    }

    if (selectedDataset != null) {
        return (
            <DatasetVersionList
                dataset={selectedDataset}
                onBack={() => setSelectedDataset(null)}
                onSelectVersion={setSelectedVersion}
            />
        );
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Datasets</h2>

                <div className="text-sm text-muted-foreground">
                    Read-only view; datasets are curated via the SDK / public REST API.
                </div>
            </div>

            <PageLoader errors={[datasetsError]} loading={datasetsIsLoading}>
                {datasets.length === 0 ? (
                    <EmptyList
                        icon={<DatabaseIcon className="size-8 text-muted-foreground" />}
                        message="Use the public REST API or SDK to create one — POST /api/ai-gateway/v1/datasets."
                        title="No datasets yet"
                    />
                ) : (
                    <div className="overflow-x-auto rounded-md border">
                        <table className="min-w-full text-sm">
                            <thead className="bg-muted/50">
                                <tr>
                                    <th className="px-3 py-2 text-left font-medium">Name</th>

                                    <th className="px-3 py-2 text-left font-medium">Description</th>

                                    <th className="px-3 py-2 text-left font-medium">Created</th>

                                    <th className="px-3 py-2 text-left font-medium">Status</th>
                                </tr>
                            </thead>

                            <tbody>
                                {datasets.map((dataset) => (
                                    <tr
                                        className="cursor-pointer border-t hover:bg-muted/30"
                                        key={dataset.id}
                                        onClick={() => setSelectedDataset(dataset)}
                                    >
                                        <td className="px-3 py-2 font-medium">
                                            <div>{dataset.name}</div>

                                            <div className="font-mono text-xs text-muted-foreground">#{dataset.id}</div>
                                        </td>

                                        <td className="px-3 py-2 text-muted-foreground">
                                            {dataset.description ?? '—'}
                                        </td>

                                        <td className="px-3 py-2 text-xs">{formatTimestamp(dataset.createdDate)}</td>

                                        <td className="px-3 py-2">
                                            {dataset.archivedDate != null ? (
                                                <span className="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">
                                                    archived
                                                </span>
                                            ) : (
                                                <span className="rounded bg-emerald-100 px-1.5 py-0.5 text-xs text-emerald-900 dark:bg-emerald-900/30 dark:text-emerald-300">
                                                    active
                                                </span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </PageLoader>
        </div>
    );
};

interface DatasetVersionListProps {
    dataset: DatasetType;
    onBack: () => void;
    onSelectVersion: (version: DatasetVersionType) => void;
}

const DatasetVersionList = ({dataset, onBack, onSelectVersion}: DatasetVersionListProps) => {
    const {
        data: versionsData,
        error: versionsError,
        isLoading: versionsIsLoading,
    } = useAiDatasetVersionsQuery({datasetId: dataset.id});

    const versions = (versionsData?.aiDatasetVersions ?? [])
        .slice()
        .sort((left, right) => (right.createdDate ?? 0) - (left.createdDate ?? 0));

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between">
                <div>
                    <button
                        className="mb-1 flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                        onClick={onBack}
                        type="button"
                    >
                        <ChevronLeftIcon className="size-4" />
                        Back to datasets
                    </button>

                    <h2 className="text-lg font-semibold">
                        {dataset.name} <span className="font-mono text-sm text-muted-foreground">#{dataset.id}</span>
                    </h2>

                    {dataset.description != null && (
                        <p className="mt-1 text-sm text-muted-foreground">{dataset.description}</p>
                    )}
                </div>
            </div>

            <PageLoader errors={[versionsError]} loading={versionsIsLoading}>
                {versions.length === 0 ? (
                    <EmptyList
                        icon={<DatabaseIcon className="size-8 text-muted-foreground" />}
                        message="The first item-add will auto-create an unfrozen version."
                        title="No versions yet"
                    />
                ) : (
                    <div className="overflow-x-auto rounded-md border">
                        <table className="min-w-full text-sm">
                            <thead className="bg-muted/50">
                                <tr>
                                    <th className="px-3 py-2 text-left font-medium">Version</th>

                                    <th className="px-3 py-2 text-left font-medium">Label</th>

                                    <th className="px-3 py-2 text-right font-medium">Items</th>

                                    <th className="px-3 py-2 text-left font-medium">Frozen</th>

                                    <th className="px-3 py-2 text-left font-medium">Created</th>
                                </tr>
                            </thead>

                            <tbody>
                                {versions.map((version) => (
                                    <tr
                                        className="cursor-pointer border-t hover:bg-muted/30"
                                        key={version.id}
                                        onClick={() => onSelectVersion(version)}
                                    >
                                        <td className="px-3 py-2 font-mono text-xs">#{version.id}</td>

                                        <td className="px-3 py-2">{version.label ?? '—'}</td>

                                        <td className="px-3 py-2 text-right font-mono">{version.itemCount}</td>

                                        <td className="px-3 py-2">
                                            {version.frozen ? (
                                                <span className="flex items-center gap-1 text-muted-foreground">
                                                    <LockIcon className="size-3" />
                                                    frozen
                                                </span>
                                            ) : (
                                                <span className="flex items-center gap-1 text-content-success-primary">
                                                    <UnlockIcon className="size-3" />
                                                    open
                                                </span>
                                            )}
                                        </td>

                                        <td className="px-3 py-2 text-xs">{formatTimestamp(version.createdDate)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </PageLoader>
        </div>
    );
};

interface DatasetVersionItemsProps {
    onBack: () => void;
    version: DatasetVersionType;
}

const DatasetVersionItems = ({onBack, version}: DatasetVersionItemsProps) => {
    const {
        data: itemsData,
        error: itemsError,
        isLoading: itemsIsLoading,
    } = useAiDatasetItemsQuery({versionId: version.id});

    const items: DatasetItemType[] = itemsData?.aiDatasetItems ?? [];

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4">
                <button
                    className="mb-1 flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                    onClick={onBack}
                    type="button"
                >
                    <ChevronLeftIcon className="size-4" />
                    Back to versions
                </button>

                <h2 className="text-lg font-semibold">
                    {`Version `}

                    <span className="font-mono">{`#${version.id}`}</span>

                    {version.label != null && <span className="text-muted-foreground">{` — ${version.label}`}</span>}
                </h2>

                <p className="mt-1 text-sm text-muted-foreground">
                    {version.itemCount} items · {version.frozen ? 'frozen' : 'open for edits via SDK'}
                </p>
            </div>

            <PageLoader errors={[itemsError]} loading={itemsIsLoading}>
                {items.length === 0 ? (
                    <EmptyList
                        icon={<DatabaseIcon className="size-8 text-muted-foreground" />}
                        message="Add items via POST /api/ai-gateway/v1/datasets/{id}/items."
                        title="No items in this version"
                    />
                ) : (
                    <div className="space-y-3">
                        {items.map((item) => (
                            <div className="rounded-md border bg-card p-4 shadow-sm" key={item.id}>
                                <div className="mb-2 flex items-center justify-between">
                                    <span className="font-mono text-xs text-muted-foreground">#{item.id}</span>

                                    {item.sourceTraceId != null && (
                                        <span className="rounded bg-secondary px-2 py-0.5 text-xs">
                                            from trace #{item.sourceTraceId}
                                        </span>
                                    )}
                                </div>

                                <div className="space-y-2 text-sm">
                                    <div>
                                        <div className="mb-1 text-xs font-semibold text-muted-foreground uppercase">
                                            Input
                                        </div>

                                        <pre className="overflow-x-auto rounded bg-muted/40 p-2 font-mono text-xs">
                                            {truncate(item.input, 800)}
                                        </pre>
                                    </div>

                                    {item.expectedOutput != null && (
                                        <div>
                                            <div className="mb-1 text-xs font-semibold text-muted-foreground uppercase">
                                                Expected output
                                            </div>

                                            <pre className="overflow-x-auto rounded bg-muted/40 p-2 font-mono text-xs">
                                                {truncate(item.expectedOutput, 400)}
                                            </pre>
                                        </div>
                                    )}

                                    {item.metadata != null && (
                                        <div>
                                            <div className="mb-1 text-xs font-semibold text-muted-foreground uppercase">
                                                Metadata
                                            </div>

                                            <pre className="overflow-x-auto rounded bg-muted/40 p-2 font-mono text-xs">
                                                {truncate(item.metadata, 400)}
                                            </pre>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </PageLoader>
        </div>
    );
};

export default AiDatasets;
