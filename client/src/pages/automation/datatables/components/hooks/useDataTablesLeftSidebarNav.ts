import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Tag, useDataTableTagsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useSearchParams} from 'react-router-dom';

interface UseDataTablesLeftSidebarNavI {
    isLoading: boolean;
    tagId: string | null;
    tags: Tag[];
}

export default function useDataTablesLeftSidebarNav(): UseDataTablesLeftSidebarNavI {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [searchParams] = useSearchParams();
    const tagId = searchParams.get('tagId');

    const {data, isLoading} = useDataTableTagsQuery({
        environmentId: String(environmentId),
        workspaceId: String(workspaceId),
    });

    const tags = (data?.dataTableTags ?? []) as Tag[];

    return {
        isLoading,
        tagId,
        tags,
    };
}
