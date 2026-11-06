import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAiObservabilitySessionsQuery} from '@/shared/middleware/graphql';
import {MessagesSquareIcon} from 'lucide-react';

interface AiObservabilitySessionsProps {
    onSelectSession: (sessionId: string) => void;
}

const AiObservabilitySessions = ({onSelectSession}: AiObservabilitySessionsProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {
        data: sessionsData,
        isError: sessionsIsError,
        isLoading: sessionsIsLoading,
    } = useAiObservabilitySessionsQuery(
        {workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: currentWorkspaceId != null}
    );

    const sessions = sessionsData?.aiObservabilitySessions ?? [];

    if (currentWorkspaceId == null || sessionsIsLoading) {
        return <PageLoader loading={true} />;
    }

    if (sessionsIsError) {
        return (
            <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
                <p className="text-destructive">Failed to load sessions. Please try again.</p>
            </div>
        );
    }

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {sessions.length === 0 ? (
                <EmptyList
                    icon={<MessagesSquareIcon className="size-12 text-muted-foreground" />}
                    message="No sessions found for this workspace."
                    title="No Sessions"
                />
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead>
                            <tr className="border-b text-muted-foreground">
                                <th className="pb-2 font-medium">Created</th>

                                <th className="pb-2 font-medium">Name</th>

                                <th className="pb-2 font-medium">User</th>

                                <th className="pb-2 font-medium">Traces</th>
                            </tr>
                        </thead>

                        <tbody>
                            {sessions.map((session) =>
                                session ? (
                                    <tr
                                        className="cursor-pointer border-b hover:bg-muted/50"
                                        key={session.id}
                                        onClick={() => onSelectSession(session.id)}
                                    >
                                        <td className="py-3 text-muted-foreground">
                                            {session.createdDate ? new Date(session.createdDate).toLocaleString() : '—'}
                                        </td>

                                        <td className="py-3 font-medium">{session.name || '—'}</td>

                                        <td className="py-3 text-muted-foreground">{session.userId || '—'}</td>

                                        <td className="py-3 text-muted-foreground">{session.traceCount ?? '—'}</td>
                                    </tr>
                                ) : null
                            )}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default AiObservabilitySessions;
