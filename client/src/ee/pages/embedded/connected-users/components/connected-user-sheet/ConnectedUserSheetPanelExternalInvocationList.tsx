import {Button} from '@/components/ui/button';
import ExternalInvocationsTable from '@/pages/automation/external-invocations/components/ExternalInvocationsTable';
import {PlatformType, useExternalInvocationLogsQuery} from '@/shared/middleware/graphql';

interface ConnectedUserSheetPanelExternalInvocationListProps {
    connectedUserId: number;
}

const ConnectedUserSheetPanelExternalInvocationList = ({
    connectedUserId,
}: ConnectedUserSheetPanelExternalInvocationListProps) => {
    const {data, isError, isLoading, refetch} = useExternalInvocationLogsQuery({
        connectedUserId,
        type: PlatformType.Embedded,
    });

    if (isLoading) {
        return <div className="py-4 text-sm text-muted-foreground">Loading...</div>;
    }

    if (isError) {
        return (
            <div className="flex flex-col items-start gap-2 py-4">
                <p className="text-sm text-destructive">External invocations could not be loaded.</p>

                <Button onClick={() => refetch()} variant="outline">
                    Retry
                </Button>
            </div>
        );
    }

    const externalInvocationLogs = data?.externalInvocationLogs.content ?? [];

    if (externalInvocationLogs.length === 0) {
        return <div className="py-4 text-sm">No external invocations have been recorded for this user yet.</div>;
    }

    return <ExternalInvocationsTable externalInvocationLogs={externalInvocationLogs} />;
};

export default ConnectedUserSheetPanelExternalInvocationList;
