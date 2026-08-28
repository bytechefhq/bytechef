import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useKnowledgeBaseTagsQuery} from '@/shared/middleware/graphql';
import {useSearchParams} from 'react-router-dom';

export default function useKnowledgeBasesLeftSidebarNav() {
    const [searchParams] = useSearchParams();

    const tagId = searchParams.get('tagId');

    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, isLoading} = useKnowledgeBaseTagsQuery({workspaceId: String(workspaceId)});

    const tags = data?.knowledgeBaseTags ?? [];

    return {
        isLoading,
        tagId,
        tags,
    };
}
