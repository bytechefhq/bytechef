import {useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

const toTitleCase = (name: string) => name.charAt(0).toUpperCase() + name.slice(1).toLowerCase();

/*
 * A sentence telling the reader that a per-environment list only shows the current environment. Returns undefined
 * where environments can't be switched (outside EE, or with a single environment), matching EnvironmentSelect.
 */
export const useEnvironmentScopeNote = (entityTitle: string): string | undefined => {
    const application = useApplicationInfoStore((state) => state.application);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const isEnterpriseEdition = application?.edition === 'EE';

    const {data: environmentsData} = useEnvironmentsQuery(undefined, {enabled: isEnterpriseEdition});

    const environments = environmentsData?.environments ?? [];

    const currentEnvironment = environments.find((environment) => environment?.id === String(currentEnvironmentId));

    if (!isEnterpriseEdition || environments.length < 2 || !currentEnvironment) {
        return undefined;
    }

    return `${entityTitle} are separate for each environment. You're viewing ${toTitleCase(currentEnvironment.name)}.`;
};
