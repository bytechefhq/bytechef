import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    useAiObservabilityExportJobsQuery,
    useCancelAiObservabilityExportJobMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {DownloadIcon} from 'lucide-react';
import {useState} from 'react';
import {toast} from 'sonner';

import AiObservabilityExportJobDialog from './AiObservabilityExportJobDialog';
import AiObservabilityWebhookSubscriptions from './AiObservabilityWebhookSubscriptions';

type ExportsTabType = 'history' | 'webhooks';

const AiObservabilityExports = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [activeTab, setActiveTab] = useState<ExportsTabType>('history');
    const [showExportDialog, setShowExportDialog] = useState(false);

    const {data: exportJobsData, isLoading: exportJobsIsLoading} = useAiObservabilityExportJobsQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const exportJobs = exportJobsData?.aiObservabilityExportJobs ?? [];

    const queryClient = useQueryClient();

    const cancelExportJobMutation = useCancelAiObservabilityExportJobMutation({
        onError: (error: Error) => toast.error(`Cancel failed: ${error.message}`),
        onSuccess: () => {
            toast.success('Export job cancelled');

            queryClient.invalidateQueries({queryKey: ['aiObservabilityExportJobs']});
        },
    });

    const handleCancelExport = (id: string) => cancelExportJobMutation.mutate({id});

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Exports</h2>

                <div className="flex gap-2">
                    <div className="flex gap-1">
                        <button
                            className={`rounded-md px-3 py-1 text-sm ${
                                activeTab === 'history'
                                    ? 'bg-primary text-primary-foreground'
                                    : 'bg-muted text-muted-foreground hover:bg-muted/80'
                            }`}
                            onClick={() => setActiveTab('history')}
                        >
                            Export History
                        </button>

                        <button
                            className={`rounded-md px-3 py-1 text-sm ${
                                activeTab === 'webhooks'
                                    ? 'bg-primary text-primary-foreground'
                                    : 'bg-muted text-muted-foreground hover:bg-muted/80'
                            }`}
                            onClick={() => setActiveTab('webhooks')}
                        >
                            Webhooks
                        </button>
                    </div>

                    {activeTab === 'history' && (
                        <button
                            className="rounded-md bg-primary px-3 py-1 text-sm text-primary-foreground hover:bg-primary/90"
                            onClick={() => setShowExportDialog(true)}
                        >
                            New Export
                        </button>
                    )}
                </div>
            </div>

            {activeTab === 'history' && (
                <>
                    {exportJobsIsLoading ? (
                        <PageLoader loading={true} />
                    ) : exportJobs.length === 0 ? (
                        <EmptyList
                            icon={<DownloadIcon className="size-12 text-muted-foreground" />}
                            message="Create an export to download observability data in CSV, JSON, or JSONL format."
                            title="No Exports Yet"
                        />
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead>
                                    <tr className="border-b text-muted-foreground">
                                        <th className="px-3 py-2 font-medium">Created</th>

                                        <th className="px-3 py-2 font-medium">Scope</th>

                                        <th className="px-3 py-2 font-medium">Format</th>

                                        <th className="px-3 py-2 font-medium">Status</th>

                                        <th className="px-3 py-2 font-medium">Records</th>

                                        <th className="px-3 py-2 font-medium">Created By</th>

                                        <th className="px-3 py-2 font-medium">Download</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {exportJobs.map((exportJob) =>
                                        exportJob ? (
                                            <tr className="border-b" key={exportJob.id}>
                                                <td className="px-3 py-2 text-muted-foreground">
                                                    {exportJob.createdDate
                                                        ? new Date(Number(exportJob.createdDate)).toLocaleString()
                                                        : '-'}
                                                </td>

                                                <td className="px-3 py-2">{exportJob.scope}</td>

                                                <td className="px-3 py-2">{exportJob.format}</td>

                                                <td className="px-3 py-2">
                                                    <span
                                                        className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                                            exportJob.status === 'COMPLETED'
                                                                ? 'bg-green-100 text-green-800'
                                                                : exportJob.status === 'FAILED'
                                                                  ? 'bg-red-100 text-red-800'
                                                                  : exportJob.status === 'PROCESSING'
                                                                    ? 'bg-blue-100 text-blue-800'
                                                                    : 'bg-yellow-100 text-yellow-800'
                                                        }`}
                                                    >
                                                        {exportJob.status}
                                                    </span>
                                                </td>

                                                <td className="px-3 py-2">
                                                    {exportJob.recordCount != null ? exportJob.recordCount : '-'}
                                                </td>

                                                <td className="px-3 py-2">{exportJob.createdBy}</td>

                                                <td className="px-3 py-2">
                                                    {exportJob.status === 'COMPLETED' && exportJob.filePath ? (
                                                        <a
                                                            className="text-primary hover:underline"
                                                            href={exportJob.filePath}
                                                        >
                                                            Download
                                                        </a>
                                                    ) : exportJob.status === 'FAILED' && exportJob.errorMessage ? (
                                                        <span
                                                            className="text-xs text-red-600"
                                                            title={exportJob.errorMessage}
                                                        >
                                                            Error
                                                        </span>
                                                    ) : exportJob.status === 'PENDING' ||
                                                      exportJob.status === 'PROCESSING' ? (
                                                        <button
                                                            className="text-xs text-muted-foreground hover:text-destructive"
                                                            disabled={cancelExportJobMutation.isPending}
                                                            onClick={() => handleCancelExport(exportJob.id)}
                                                            type="button"
                                                        >
                                                            Cancel
                                                        </button>
                                                    ) : (
                                                        '-'
                                                    )}
                                                </td>
                                            </tr>
                                        ) : null
                                    )}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {showExportDialog && <AiObservabilityExportJobDialog onClose={() => setShowExportDialog(false)} />}
                </>
            )}

            {activeTab === 'webhooks' && <AiObservabilityWebhookSubscriptions />}
        </div>
    );
};

export default AiObservabilityExports;
