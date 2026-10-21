import Badge from '@/components/Badge/Badge';
import {
    APP_DEPLOYMENT_POLL_INTERVAL_MILLIS,
    APP_DEPLOYMENT_STATUS_LABELS,
    APP_DEPLOYMENT_STATUS_STYLES,
    isAppDeploymentInProgress,
} from '@/pages/automation/apps/constants';
import {AppInstanceDeploymentSummaryQuery, useAppInstanceDeploymentSummaryQuery} from '@/shared/middleware/graphql';

export const appDeploymentSummaryRefetchInterval = (query: {state: {data?: AppInstanceDeploymentSummaryQuery}}) => {
    const latestDeployment = query.state.data?.appInstance.deployments[0];

    return latestDeployment && isAppDeploymentInProgress(latestDeployment.status)
        ? APP_DEPLOYMENT_POLL_INTERVAL_MILLIS
        : false;
};

interface AppDeploymentSummaryProps {
    appInstanceId: string;
}

const AppDeploymentSummary = ({appInstanceId}: AppDeploymentSummaryProps) => {
    const {data, error, isLoading} = useAppInstanceDeploymentSummaryQuery(
        {id: appInstanceId},
        {refetchInterval: appDeploymentSummaryRefetchInterval}
    );

    const currentDeployment = data?.appInstance.currentDeployment;
    const latestDeployment = data?.appInstance.deployments[0];

    if (isLoading) {
        return null;
    }

    if (error) {
        return <p className="text-sm text-content-neutral-secondary">Deployment status unavailable</p>;
    }

    if (!currentDeployment && !latestDeployment) {
        return null;
    }

    const latestDeploymentDate = latestDeployment?.finishedDate || latestDeployment?.createdDate;

    return (
        <dl className="flex flex-wrap items-center gap-x-6 gap-y-1 text-sm">
            <div className="flex items-center gap-2">
                <dt className="text-content-neutral-secondary">Current version</dt>

                <dd className="font-medium">{currentDeployment?.appVersion.name || '-'}</dd>
            </div>

            {latestDeployment && (
                <div className="flex items-center gap-2">
                    <dt className="text-content-neutral-secondary">Latest deployment</dt>

                    <dd className="flex items-center gap-2">
                        <Badge
                            label={APP_DEPLOYMENT_STATUS_LABELS[latestDeployment.status]}
                            styleType={APP_DEPLOYMENT_STATUS_STYLES[latestDeployment.status]}
                            weight="semibold"
                        />

                        {latestDeploymentDate && <span>{new Date(latestDeploymentDate).toLocaleString()}</span>}
                    </dd>
                </div>
            )}
        </dl>
    );
};

export default AppDeploymentSummary;
