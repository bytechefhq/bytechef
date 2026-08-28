import {DataTableTagsEntry, Tag} from '@/shared/middleware/graphql';
import {useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

interface UseDataTablesFilterTitleProps {
    allTags: Tag[];
    tagsByTableData: DataTableTagsEntry[];
}

interface UseDataTablesFilterTitleI {
    pageTitle: string | undefined;
    tagId: string | null;
}

export default function useDataTablesFilterTitle({
    allTags,
    tagsByTableData,
}: UseDataTablesFilterTitleProps): UseDataTablesFilterTitleI {
    const [searchParams] = useSearchParams();

    const tagId = searchParams.get('tagId');

    const pageTitle = useMemo(() => {
        if (!tagId) return undefined;

        const fromGlobal = allTags.find((tag) => tag.id === tagId)?.name;

        if (fromGlobal) {
            return fromGlobal;
        }

        const flatTags = tagsByTableData.flatMap((entry) => entry.tags ?? []);

        return flatTags.find((tag) => tag.id === tagId)?.name;
    }, [allTags, tagsByTableData, tagId]);

    return {
        pageTitle,
        tagId,
    };
}
