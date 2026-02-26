import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useKnowledgeBasesQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useParams} from 'react-router-dom';

export default function useKnowledgeBaseLeftSidebarNav() {
    const {id} = useParams<{id: string}>();
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const workspaceId = String(currentWorkspaceId ?? 1049);

    const {data, isLoading} = useKnowledgeBasesQuery({
        environmentId: String(environmentId),
        workspaceId,
    });

    const knowledgeBases = (data?.knowledgeBases ?? []).filter((kb): kb is NonNullable<typeof kb> => kb !== null);

    return {
        currentKnowledgeBaseId: id,
        isLoading,
        knowledgeBases,
    };
}
