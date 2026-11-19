import {IntegrationStatus, IntegrationVersion} from '@/ee/shared/middleware/embedded/configuration';
import {useGetIntegrationVersionsQuery} from '@/ee/shared/queries/embedded/integrationVersions.queries';

export const isNewerIntegrationVersionAvailable = (
    integrationVersions: IntegrationVersion[] | undefined,
    integrationVersion: number | null | undefined
) => {
    if (integrationVersion == null) {
        return false;
    }

    const lastPublishedIntegrationVersion = (integrationVersions ?? [])
        .filter((version) => version.status === IntegrationStatus.Published)
        .reduce((lastVersion, version) => Math.max(lastVersion, version.version ?? 0), 0);

    return lastPublishedIntegrationVersion > integrationVersion;
};

const useNewerIntegrationVersionAvailable = (
    integrationId: number | null | undefined,
    integrationVersion: number | null | undefined
) => {
    const {data: integrationVersions} = useGetIntegrationVersionsQuery(integrationId ?? 0, integrationId != null);

    return isNewerIntegrationVersionAvailable(integrationVersions, integrationVersion);
};

export default useNewerIntegrationVersionAvailable;
