import {Tag, useDataTableTagsQuery} from '@/shared/middleware/graphql';
import {useSearchParams} from 'react-router-dom';

interface UseDataTablesLeftSidebarNavI {
    isLoading: boolean;
    tagId: string | null;
    tags: Tag[];
}

export default function useDataTablesLeftSidebarNav(): UseDataTablesLeftSidebarNavI {
    const [searchParams] = useSearchParams();
    const tagId = searchParams.get('tagId');

    const {data, isLoading} = useDataTableTagsQuery();

    const tags = (data?.dataTableTags ?? []) as Tag[];

    return {
        isLoading,
        tagId,
        tags,
    };
}
