import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ContextStoreSource, useContextStoreSourcesQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

interface UseContextStoreSourcesResultI {
    error: unknown;
    isLoading: boolean;
    sources: ContextStoreSource[];
}

export default function useContextStoreSources(): UseContextStoreSourcesResultI {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, error, isLoading} = useContextStoreSourcesQuery({
        environmentId: String(environmentId),
        workspaceId: String(workspaceId),
    });

    const sources = useMemo(
        () =>
            (data?.contextStoreSources ?? []).filter(
                (source): source is NonNullable<typeof source> => source !== null
            ) as ContextStoreSource[],
        [data?.contextStoreSources]
    );

    return {
        error,
        isLoading,
        sources,
    };
}
