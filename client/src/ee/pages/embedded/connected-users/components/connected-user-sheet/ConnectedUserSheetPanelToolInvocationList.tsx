import {Button} from '@/components/ui/button';
import ToolInvocationsTable from '@/pages/automation/tool-invocations/components/ToolInvocationsTable';
import {PlatformType, useToolInvocationLogsQuery} from '@/shared/middleware/graphql';

interface ConnectedUserSheetPanelToolInvocationListProps {
    connectedUserId: number;
}

const ConnectedUserSheetPanelToolInvocationList = ({
    connectedUserId,
}: ConnectedUserSheetPanelToolInvocationListProps) => {
    const {data, isError, isLoading, refetch} = useToolInvocationLogsQuery({
        connectedUserId,
        type: PlatformType.Embedded,
    });

    if (isLoading) {
        return <div className="py-4 text-sm text-muted-foreground">Loading...</div>;
    }

    if (isError) {
        return (
            <div className="flex flex-col items-start gap-2 py-4">
                <p className="text-sm text-destructive">Tool invocations could not be loaded.</p>

                <Button onClick={() => refetch()} variant="outline">
                    Retry
                </Button>
            </div>
        );
    }

    const toolInvocationLogs = data?.toolInvocationLogs.content ?? [];

    if (toolInvocationLogs.length === 0) {
        return <div className="py-4 text-sm">No tool invocations have been recorded for this user yet.</div>;
    }

    return <ToolInvocationsTable toolInvocationLogs={toolInvocationLogs} />;
};

export default ConnectedUserSheetPanelToolInvocationList;
