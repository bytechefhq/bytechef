import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

const toTitleCase = (name: string) => name.charAt(0).toUpperCase() + name.slice(1).toLowerCase();

/*
 * A sentence telling the reader that a per-environment list only shows the current environment. Counts and names
 * environments the way EnvironmentSelect does, but returns undefined unless there is another one to switch to
 * (outside EE, or with a single environment).
 */
export const useEnvironmentScopeNote = (entityTitle: string): string | undefined => {
    const application = useApplicationInfoStore((state) => state.application);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    // Same query as the sidebar's EnvironmentSelect, so it is served from that cache.
    const {data: environmentsData} = useEnvironmentsQuery();

    const selectableEnvironments = (environmentsData?.environments ?? []).filter(
        (environment) => environment?.id != null && ENVIRONMENT_CONFIGS[+environment.id]
    );

    const currentEnvironmentConfig = ENVIRONMENT_CONFIGS[currentEnvironmentId];

    if (application?.edition !== 'EE' || selectableEnvironments.length < 2 || !currentEnvironmentConfig) {
        return undefined;
    }

    return `${entityTitle} are separate for each environment. You're viewing ${toTitleCase(currentEnvironmentConfig.label)}.`;
};
