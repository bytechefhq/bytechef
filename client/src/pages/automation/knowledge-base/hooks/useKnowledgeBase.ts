import {KnowledgeBase, KnowledgeBaseDocument, useKnowledgeBaseQuery} from '@/shared/middleware/graphql';
import {useNavigate, useParams} from 'react-router-dom';

interface UseKnowledgeBaseResultI {
    documents: KnowledgeBaseDocument[];
    error: unknown;
    handleBackClick: () => void;
    isLoading: boolean;
    knowledgeBase: (KnowledgeBase & {documents: KnowledgeBaseDocument[]}) | undefined;
    knowledgeBaseId: string;
}

export default function useKnowledgeBase(): UseKnowledgeBaseResultI {
    const {id} = useParams<{id: string}>();
    const navigate = useNavigate();

    const knowledgeBaseId = id ?? '';

    const {data, error, isLoading} = useKnowledgeBaseQuery(
        {id: knowledgeBaseId},
        {
            enabled: !!knowledgeBaseId,
        }
    );

    const knowledgeBase = data?.knowledgeBase ?? undefined;
    const documents = (knowledgeBase?.documents ?? []).filter((doc): doc is NonNullable<typeof doc> => doc !== null);

    const handleBackClick = () => {
        navigate('/automation/knowledge-bases');
    };

    const knowledgeBaseWithDocuments = knowledgeBase
        ? {
              ...knowledgeBase,
              documents,
          }
        : undefined;

    return {
        documents,
        error,
        handleBackClick,
        isLoading,
        knowledgeBase: knowledgeBaseWithDocuments,
        knowledgeBaseId,
    };
}
