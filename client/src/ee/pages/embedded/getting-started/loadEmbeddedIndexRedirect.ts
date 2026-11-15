import {AUTHORITIES} from '@/shared/constants';
import {
    EmbeddedGettingStartedCompletedDocument,
    EmbeddedGettingStartedCompletedQuery,
    EmbeddedGettingStartedCompletedQueryVariables,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {gettingStartedStore, isGettingStartedCompleted} from '@/shared/stores/useGettingStartedStore';
import {QueryClient} from '@tanstack/react-query';

import resolveEmbeddedIndexRedirect, {EMBEDDED_DEFAULT_ROUTE_PATH} from './resolveEmbeddedIndexRedirect';

const ensureApplicationInfoLoaded = async () => {
    const {application, getApplicationInfo} = applicationInfoStore.getState();

    if (!application) {
        await getApplicationInfo();
    }
};

const ensureAccountLoaded = async () => {
    const {getAccount, sessionHasBeenFetched} = authenticationStore.getState();

    if (!sessionHasBeenFetched) {
        await getAccount();
    }
};

export default async function loadEmbeddedIndexRedirect(queryClient: QueryClient): Promise<string> {
    try {
        await Promise.all([ensureApplicationInfoLoaded(), ensureAccountLoaded()]);
    } catch {
        return EMBEDDED_DEFAULT_ROUTE_PATH;
    }

    const {application} = applicationInfoStore.getState();
    const {account, authenticated} = authenticationStore.getState();

    return resolveEmbeddedIndexRedirect({
        edition: application?.edition,
        environmentId: environmentStore.getState().currentEnvironmentId,
        fetchCompleted: async () => {
            if (isGettingStartedCompleted('embedded')) {
                return true;
            }

            const data = await queryClient.query({
                queryFn: fetcher<EmbeddedGettingStartedCompletedQuery, EmbeddedGettingStartedCompletedQueryVariables>(
                    EmbeddedGettingStartedCompletedDocument
                ),
                queryKey: ['embeddedGettingStartedCompleted'],
            });

            if (data.embeddedGettingStartedCompleted) {
                gettingStartedStore.getState().complete('embedded', 'COMPLETED');
            }

            return data.embeddedGettingStartedCompleted;
        },
        isAdmin: authenticated && !!account?.authorities?.includes(AUTHORITIES.ADMIN),
    });
}
