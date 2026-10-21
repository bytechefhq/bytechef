import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import ExternalInvocationsTable from '@/pages/automation/external-invocations/components/ExternalInvocationsTable';
import {parseAppId} from '@/pages/automation/external-invocations/utils/parseAppId';
import WorkflowExecutionSheet from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {toEnvironmentName} from '@/shared/constants';
import {
    AppInstance,
    PlatformType,
    WorkspaceExternalInvocationLogsQuery,
    useWorkspaceExternalInvocationLogsQuery,
} from '@/shared/middleware/graphql';
import {ActivityIcon} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';

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

const getEnvironmentName = (appInstance: AppActivityInstanceType) =>
    toEnvironmentName(Number(appInstance.environmentId));

const AppActivityEmptyState = ({appInstance}: {appInstance: AppActivityInstanceType}) => (
    <EmptyState
        description={`Every call this instance makes through ByteChef in the ${getEnvironmentName(appInstance)} environment appears here: actions, tools, proxy requests and workflow runs.`}
        icon={<ActivityIcon />}
        title="No activity yet"
    />
);

const AppActivityLogList = ({appId, appInstance}: AppActivityLogListProps) => {
    const [loadedLogs, setLoadedLogs] = useState<Array<ExternalInvocationLogItemType>>([]);
    const [pageNumber, setPageNumber] = useState(0);

    const setWorkflowExecutionId = useWorkflowExecutionSheetStore((state) => state.setWorkflowExecutionId);
    const setWorkflowExecutionSheetOpen = useWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

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

    const handleViewJob = useCallback(
        (jobId: number) => {
            setWorkflowExecutionId(jobId, 'JOB');
            setWorkflowExecutionSheetOpen(true);
        },
        [setWorkflowExecutionId, setWorkflowExecutionSheetOpen]
    );

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

            {!isLoading && !error && loadedLogs.length === 0 && <AppActivityEmptyState appInstance={appInstance} />}

            {loadedLogs.length > 0 && (
                <>
                    <p className="flex min-h-9 items-center text-sm text-content-neutral-secondary">
                        {`The invocations of the selected instance of this app, in the ${getEnvironmentName(appInstance)} environment.`}
                    </p>

                    <ExternalInvocationsTable externalInvocationLogs={loadedLogs} onViewJob={handleViewJob} showUser />
                </>
            )}

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
        <div className="flex flex-1 flex-col gap-4">
            {parsedAppId === undefined ? (
                <AppActivityEmptyState appInstance={appInstance} />
            ) : (
                <AppActivityLogList
                    appId={parsedAppId}
                    appInstance={appInstance}
                    key={`${appInstance.workspaceId}-${appInstance.environmentId}-${appInstance.id}-${parsedAppId}`}
                />
            )}

            <WorkflowExecutionSheet />
        </div>
    );
};

export default AppActivityTab;
