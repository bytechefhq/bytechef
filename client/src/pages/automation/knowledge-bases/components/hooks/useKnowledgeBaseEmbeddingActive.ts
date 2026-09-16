import {useKnowledgeBaseEmbeddingActiveQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

export default function useKnowledgeBaseEmbeddingActive() {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data, isLoading} = useKnowledgeBaseEmbeddingActiveQuery({
        environment: currentEnvironmentId,
    });

    return {
        embeddingActive: data?.knowledgeBaseEmbeddingActive ?? true,
        isLoading,
    };
}
