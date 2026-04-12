import {useKnowledgeBaseTagsQuery} from '@/shared/middleware/graphql';
import {useSearchParams} from 'react-router-dom';

export default function useKnowledgeBasesLeftSidebarNav() {
    const [searchParams] = useSearchParams();

    const tagId = searchParams.get('tagId');

    const {data, isLoading} = useKnowledgeBaseTagsQuery();

    const tags = data?.knowledgeBaseTags ?? [];

    return {
        isLoading,
        tagId,
        tags,
    };
}
