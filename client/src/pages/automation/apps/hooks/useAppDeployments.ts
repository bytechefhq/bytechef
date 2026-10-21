import {APP_DEPLOYMENT_POLL_INTERVAL_MILLIS, isAppDeploymentInProgress} from '@/pages/automation/apps/constants';
import {AppDeploymentsQuery, useAppDeploymentsQuery} from '@/shared/middleware/graphql';
import {keepPreviousData} from '@tanstack/react-query';

export type AppDeploymentItemType = AppDeploymentsQuery['appDeployments'][number];

export const appDeploymentsRefetchInterval = (query: {state: {data?: AppDeploymentsQuery}}) =>
    (query.state.data?.appDeployments ?? []).some((appDeployment) => isAppDeploymentInProgress(appDeployment.status))
        ? APP_DEPLOYMENT_POLL_INTERVAL_MILLIS
        : false;

export default function useAppDeployments(appInstanceId: string, limit: number) {
    const {data, error, isLoading} = useAppDeploymentsQuery(
        {appInstanceId, limit},
        {placeholderData: keepPreviousData, refetchInterval: appDeploymentsRefetchInterval}
    );

    return {appDeployments: data?.appDeployments ?? [], error, isLoading};
}
