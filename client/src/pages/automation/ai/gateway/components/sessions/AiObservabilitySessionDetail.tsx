import PageLoader from '@/components/PageLoader';
import {useAiObservabilitySessionQuery} from '@/shared/middleware/graphql';
import {ArrowLeftIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface AiObservabilitySessionDetailProps {
    onBack: () => void;
    sessionId: string;
}

const TRACE_STATUS_CLASSES: Record<string, string> = {
    ACTIVE: 'bg-blue-100 text-blue-800',
    COMPLETED: 'bg-green-100 text-green-800',
    ERROR: 'bg-red-100 text-red-800',
};

const AiObservabilitySessionDetail = ({onBack, sessionId}: AiObservabilitySessionDetailProps) => {
    const {
        data: sessionData,
        isError: sessionIsError,
        isLoading: sessionIsLoading,
    } = useAiObservabilitySessionQuery({id: sessionId});

    const session = sessionData?.aiObservabilitySession;

    if (sessionIsLoading) {
        return <PageLoader loading={true} />;
    }

    if (sessionIsError) {
        return (
            <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                <button
                    className="mb-4 flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
                    onClick={onBack}
                >
                    <ArrowLeftIcon className="size-4" />

                    <span>Back to Sessions</span>
                </button>

                <p className="text-destructive">Failed to load session. Please try again.</p>
            </div>
        );
    }

    if (!session) {
        return (
            <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                <button
                    className="mb-4 flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
                    onClick={onBack}
                >
                    <ArrowLeftIcon className="size-4" />

                    <span>Back to Sessions</span>
                </button>

                <p className="text-muted-foreground">Session not found.</p>
            </div>
        );
    }

    const traces = session.traces ?? [];

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <button
                className="mb-4 flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
                onClick={onBack}
            >
                <ArrowLeftIcon className="size-4" />

                <span>Back to Sessions</span>
            </button>

            <div className="mb-6 rounded-lg border bg-card p-6">
                <h2 className="mb-4 text-xl font-semibold">{session.name || 'Unnamed Session'}</h2>

                <div className="flex flex-wrap gap-6 text-sm text-muted-foreground">
                    {session.userId && (
                        <div>
                            <span className="font-medium">User:</span> {session.userId}
                        </div>
                    )}

                    <div>
                        <span className="font-medium">Traces:</span> {traces.length}
                    </div>
                </div>
            </div>

            <h3 className="mb-3 text-lg font-semibold">Traces</h3>

            {traces.length === 0 ? (
                <p className="text-muted-foreground">No traces recorded for this session.</p>
            ) : (
                <div className="space-y-2">
                    {traces.map((trace) =>
                        trace ? (
                            <div className="flex items-center gap-4 rounded-lg border bg-card p-4" key={trace.id}>
                                <span
                                    className={twMerge(
                                        'rounded-full px-2 py-0.5 text-xs font-medium',
                                        TRACE_STATUS_CLASSES[trace.status] || 'bg-gray-100 text-gray-800'
                                    )}
                                >
                                    {trace.status}
                                </span>

                                <span className="font-medium">{trace.name || '—'}</span>

                                <div className="ml-auto flex items-center gap-4 text-sm text-muted-foreground">
                                    {trace.totalLatencyMs != null && <span>{trace.totalLatencyMs}ms</span>}

                                    {trace.totalCost != null && <span>${trace.totalCost.toFixed(6)}</span>}

                                    {trace.createdDate && <span>{new Date(trace.createdDate).toLocaleString()}</span>}
                                </div>
                            </div>
                        ) : null
                    )}
                </div>
            )}
        </div>
    );
};

export default AiObservabilitySessionDetail;
