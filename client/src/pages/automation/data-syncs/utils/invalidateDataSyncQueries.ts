import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {type QueryClient} from '@tanstack/react-query';

interface InvalidateDataSyncQueriesOptionsI {
    projects?: boolean;
}

export default function invalidateDataSyncQueries(
    queryClient: QueryClient,
    options: InvalidateDataSyncQueriesOptionsI = {}
) {
    queryClient.invalidateQueries({queryKey: ['dataSync']});
    queryClient.invalidateQueries({queryKey: ['dataSyncs']});

    if (options.projects) {
        queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
    }
}
