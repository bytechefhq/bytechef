import Button from '@/components/Button/Button';
import ExternalInvocationsTable from '@/pages/automation/external-invocations/components/ExternalInvocationsTable';
import {parseAppId} from '@/pages/automation/external-invocations/utils/parseAppId';
import {toEnvironmentName} from '@/shared/constants';
import {
    AppInstance,
    PlatformType,
    WorkspaceExternalInvocationLogsQuery,
    useWorkspaceExternalInvocationLogsQuery,
} from '@/shared/middleware/graphql';
import {ArrowUpRightIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';

type ExternalInvocationLogItemType =
    WorkspaceExternalInvocationLogsQuery['workspaceExternalInvocationLogs']['content'][number];

type AppActivityInstanceType = Pick<AppInstance, 'environmentId' | 'id' | 'workspaceId'>;

interface AppActivityTabProps {
    appId: string;
    appInstance: AppActivityInstanceType;
}

interface AppActivityLogListProps {
    appId: number;
    appInstance: AppActivityInstanceType;
}

const AppActivityLogList = ({appId, appInstance}: AppActivityLogListProps) => {
    const [loadedLogs, setLoadedLogs] = useState<Array<ExternalInvocationLogItemType>>([]);
    const [pageNumber, setPageNumber] = useState(0);

    const {data, error, isLoading} = useWorkspaceExternalInvocationLogsQuery({
        appId,
        appInstanceId: appInstance.id,
        environmentId: appInstance.environmentId,
        page: pageNumber,
        type: PlatformType.Automation,
        workspaceId: appInstance.workspaceId,
    });

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

            {!isLoading && error && loadedLogs.length === 0 && (
                <div className="p-4 text-sm text-content-destructive">
                    The activity of this app could not be loaded.
                </div>
            )}

            {!isLoading && !error && loadedLogs.length === 0 && (
                <div className="p-4 text-sm text-content-neutral-secondary">
                    No invocations recorded for this app yet.
                </div>
            )}

            {loadedLogs.length > 0 && <ExternalInvocationsTable externalInvocationLogs={loadedLogs} showUser />}

            {pageNumber + 1 < totalPages && (
                <div className="flex justify-center">
                    <Button label="Load more" onClick={() => setPageNumber(pageNumber + 1)} variant="outline" />
                </div>
            )}
        </>
    );
};

const AppActivityTab = ({appId, appInstance}: AppActivityTabProps) => {
    const parsedAppId = parseAppId(appId);

    return (
        <div className="flex flex-col gap-4">
            <p className="text-sm text-content-neutral-secondary">
                {`The invocations of the selected instance of this app, in the ${toEnvironmentName(Number(appInstance.environmentId))} environment.`}
            </p>

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
                    appInstance={appInstance}
                    key={`${appInstance.workspaceId}-${appInstance.environmentId}-${appInstance.id}-${parsedAppId}`}
                />
            )}
        </div>
    );
};

export default AppActivityTab;
