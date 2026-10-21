import Button from '@/components/Button/Button';
import ExternalInvocationsTable from '@/pages/automation/external-invocations/components/ExternalInvocationsTable';
import {parseAppId} from '@/pages/automation/external-invocations/utils/parseAppId';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    PlatformType,
    WorkspaceExternalInvocationLogsQuery,
    useWorkspaceExternalInvocationLogsQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ArrowUpRightIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';

type ExternalInvocationLogItemType =
    WorkspaceExternalInvocationLogsQuery['workspaceExternalInvocationLogs']['content'][number];

interface AppActivityTabProps {
    appId: string;
}

const AppActivityLogList = ({appId}: {appId: number}) => {
    const [loadedLogs, setLoadedLogs] = useState<Array<ExternalInvocationLogItemType>>([]);
    const [pageNumber, setPageNumber] = useState(0);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, isLoading} = useWorkspaceExternalInvocationLogsQuery(
        {
            appId,
            environmentId: String(currentEnvironmentId),
            page: pageNumber,
            type: PlatformType.Automation,
            workspaceId: String(currentWorkspaceId),
        },
        {enabled: currentEnvironmentId != null && currentWorkspaceId != null}
    );

    const externalInvocationLogPage = data?.workspaceExternalInvocationLogs;
    const totalPages = externalInvocationLogPage?.totalPages ?? 0;

    useEffect(() => {
        if (!externalInvocationLogPage) {
            return;
        }

        setLoadedLogs((previousLogs) => {
            const previousIds = new Set(previousLogs.map((externalInvocationLog) => externalInvocationLog.id));

            return [
                ...previousLogs,
                ...externalInvocationLogPage.content.filter(
                    (externalInvocationLog) => !previousIds.has(externalInvocationLog.id)
                ),
            ];
        });
    }, [externalInvocationLogPage]);

    return (
        <>
            {isLoading && <div className="p-4 text-sm text-content-neutral-secondary">Loading...</div>}

            {!isLoading && loadedLogs.length === 0 && (
                <div className="p-4 text-sm text-content-neutral-secondary">
                    No invocations recorded for this app yet.
                </div>
            )}

            {loadedLogs.length > 0 && <ExternalInvocationsTable externalInvocationLogs={loadedLogs} />}

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
                        to={`/automation/external-invocations?appId=${parsedAppId}`}
                    >
                        View in External Invocations
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
