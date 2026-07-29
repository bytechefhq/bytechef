import ToolInvocationsTable from '@/pages/automation/tool-invocations/components/ToolInvocationsTable';
import {useToolInvocationLogsQuery} from '@/shared/middleware/graphql';

interface ConnectedUserSheetPanelToolInvocationListProps {
    connectedUserId: number;
}

const ConnectedUserSheetPanelToolInvocationList = ({
    connectedUserId,
}: ConnectedUserSheetPanelToolInvocationListProps) => {
    const {data, isLoading} = useToolInvocationLogsQuery({connectedUserId});

    const toolInvocationLogs = data?.toolInvocationLogs.content ?? [];

    if (isLoading) {
        return <div className="p-4 text-sm text-content-neutral-secondary">Loading...</div>;
    }

    if (toolInvocationLogs.length === 0) {
        return (
            <div className="p-4 text-sm text-content-neutral-secondary">
                No tool invocations have been recorded for this user yet.
            </div>
        );
    }

    return <ToolInvocationsTable toolInvocationLogs={toolInvocationLogs} />;
};

export default ConnectedUserSheetPanelToolInvocationList;
