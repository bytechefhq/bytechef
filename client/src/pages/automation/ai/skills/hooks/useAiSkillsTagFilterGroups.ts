import {type FilterGroupI} from '@/shared/components/filters/FilterMenu';
import {Tag, useAiSkillTagsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';
import {useSearchParams} from 'react-router-dom';

// Sentinel for the cleared filter. Tag ids are numeric strings, so this can never collide with one.
export const ALL_TAGS = 'ALL_TAGS';

/**
 * The Tags facet of the skills filter menu, as a {@link FilterGroupI}.
 *
 * The selection lives in the `?tagId=` search param rather than component state, so a filtered list stays
 * linkable and survives a reload — as it did when this was a sidebar nav.
 *
 * Returns an empty group list while no tags exist, so the filter control disappears rather than offering a
 * facet with nothing but "All tags" in it.
 */
export default function useAiSkillsTagFilterGroups(): FilterGroupI[] {
    const [searchParams, setSearchParams] = useSearchParams();

    const {data: tagsData} = useAiSkillTagsQuery();

    // Memoised so the group array below has a stable dependency: the `?? []` fallback would otherwise be a
    // fresh array on every render where the query has not resolved.
    const tags = useMemo(() => (tagsData?.aiSkillTags ?? []) as Tag[], [tagsData?.aiSkillTags]);

    const tagId = searchParams.get('tagId') ?? ALL_TAGS;

    return useMemo(() => {
        if (tags.length === 0) {
            return [];
        }

        return [
            {
                allValue: ALL_TAGS,
                key: 'tags',
                label: 'Tags',
                onChange: (value: string) => {
                    const nextParams = new URLSearchParams(searchParams);

                    if (value === ALL_TAGS) {
                        nextParams.delete('tagId');
                    } else {
                        nextParams.set('tagId', value);
                    }

                    setSearchParams(nextParams);
                },
                options: [
                    {label: 'All tags', value: ALL_TAGS},
                    ...tags.map((tag) => ({label: tag.name, value: String(tag.id)})),
                ],
                value: tagId,
            },
        ];
    }, [searchParams, setSearchParams, tagId, tags]);
}
