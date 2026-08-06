import {AiSkill, Tag, useAiSkillTagsQuery, useAiSkillsQuery} from '@/shared/middleware/graphql';
import {useMemo, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

interface UseAiSkillsLeftSidebarI {
    error: unknown;
    filteredSkills: AiSkill[];
    handleSearchChange: (value: string) => void;
    isLoading: boolean;
    search: string;
    tagId: string | null;
    tags: Tag[];
}

/**
 * Loads the skills list for the skill-detail sidebar (the "data tables" style sidebar that appears
 * once the user opens an individual skill). Search state is local to the sidebar — it's intentionally
 * NOT wired to the global useAiSkillsStore.searchQuery so the main Skills list page and the sidebar
 * can be filtered independently.
 */
export default function useAiSkillsLeftSidebar(): UseAiSkillsLeftSidebarI {
    const [search, setSearch] = useState('');

    const [searchParams] = useSearchParams();
    const tagId = searchParams.get('tagId');

    const {data, error, isLoading} = useAiSkillsQuery();

    const {data: tagsData} = useAiSkillTagsQuery();

    const collator = useMemo(() => new Intl.Collator(undefined, {numeric: true, sensitivity: 'base'}), []);

    const filteredSkills = useMemo(() => {
        const query = search.trim().toLowerCase();
        const skills = [...(data?.aiSkills ?? [])].sort((skillA, skillB) =>
            collator.compare(skillA.name.trim(), skillB.name.trim())
        );

        const tagFilteredSkills = tagId
            ? skills.filter((skill) => (skill.tags ?? []).some((tag) => String(tag.id) === tagId))
            : skills;

        if (!query) {
            return tagFilteredSkills;
        }

        return tagFilteredSkills.filter((skill) => skill.name.toLowerCase().includes(query));
    }, [data, search, collator, tagId]);

    return {
        error,
        filteredSkills,
        handleSearchChange: setSearch,
        isLoading,
        search,
        tagId,
        tags: (tagsData?.aiSkillTags ?? []) as Tag[],
    };
}
