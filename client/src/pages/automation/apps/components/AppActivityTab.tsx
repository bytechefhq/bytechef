import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ToolInvocationsTable from '@/pages/automation/tool-invocations/components/ToolInvocationsTable';
import {parseAppId} from '@/pages/automation/tool-invocations/utils/parseAppId';
import {WorkspaceToolInvocationLogsQuery, useWorkspaceToolInvocationLogsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ArrowUpRightIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';

type ToolInvocationLogItemType = WorkspaceToolInvocationLogsQuery['workspaceToolInvocationLogs']['content'][number];

interface AppActivityTabProps {
    appId: string;
}

const AppActivityLogList = ({appId}: {appId: number}) => {
    const [loadedLogs, setLoadedLogs] = useState<Array<ToolInvocationLogItemType>>([]);
    const [pageNumber, setPageNumber] = useState(0);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, isLoading} = useWorkspaceToolInvocationLogsQuery(
        {
            appId,
            environmentId: String(currentEnvironmentId),
            page: pageNumber,
            workspaceId: String(currentWorkspaceId),
        },
        {enabled: currentEnvironmentId != null && currentWorkspaceId != null}
    );

    const toolInvocationLogPage = data?.workspaceToolInvocationLogs;
    const totalPages = toolInvocationLogPage?.totalPages ?? 0;

    useEffect(() => {
        if (!toolInvocationLogPage) {
            return;
        }

        setLoadedLogs((previousLogs) => {
            const previousIds = new Set(previousLogs.map((toolInvocationLog) => toolInvocationLog.id));

            return [
                ...previousLogs,
                ...toolInvocationLogPage.content.filter((toolInvocationLog) => !previousIds.has(toolInvocationLog.id)),
            ];
        });
    }, [toolInvocationLogPage]);

    return (
        <>
            {isLoading && <div className="p-4 text-sm text-content-neutral-secondary">Loading...</div>}

            {!isLoading && loadedLogs.length === 0 && (
                <div className="p-4 text-sm text-content-neutral-secondary">
                    No invocations recorded for this app yet.
                </div>
            )}

            {loadedLogs.length > 0 && <ToolInvocationsTable toolInvocationLogs={loadedLogs} />}

            {pageNumber + 1 < totalPages && (
                <div className="flex justify-center">
                    <Button label="Load more" onClick={() => setPageNumber(pageNumber + 1)} variant="outline" />
                </div>
            )}
        </>
    );
};

const AppActivityTab = ({appId}: AppActivityTabProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const parsedAppId = parseAppId(appId);

    return (
        <div className="flex flex-col gap-4">
            {parsedAppId !== undefined && (
                <div className="flex justify-end">
                    <Link
                        className="flex items-center gap-1 text-sm text-content-brand-primary hover:underline"
                        to={`/automation/executions/tool-invocations?appId=${parsedAppId}`}
                    >
                        View in Tool Invocations
                        <ArrowUpRightIcon className="size-4" />
                    </Link>
                </div>
            )}

            {parsedAppId === undefined ? (
                <div className="p-4 text-sm text-content-neutral-secondary">
                    No invocations recorded for this app yet.
                </div>
            ) : (
                <AppActivityLogList
                    appId={parsedAppId}
                    key={`${currentWorkspaceId}-${currentEnvironmentId}-${parsedAppId}`}
                />
            )}
        </div>
    );
};

export default AppActivityTab;
