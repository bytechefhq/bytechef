import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useKnowledgeBaseTagsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useSearchParams} from 'react-router-dom';

export default function useKnowledgeBasesLeftSidebarNav() {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [searchParams] = useSearchParams();

    const tagId = searchParams.get('tagId');

    const {data, isLoading} = useKnowledgeBaseTagsQuery({
        environmentId: String(environmentId),
        workspaceId: String(workspaceId),
    });

    const tags = data?.knowledgeBaseTags ?? [];

    return {
        isLoading,
        tagId,
        tags,
    };
}
