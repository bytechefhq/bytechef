import {AiSkill, useAiSkillsQuery} from '@/shared/middleware/graphql';
import {useMemo, useState} from 'react';

interface UseAiSkillsLeftSidebarI {
    error: unknown;
    filteredSkills: AiSkill[];
    handleSearchChange: (value: string) => void;
    isLoading: boolean;
    search: string;
}

/**
 * Loads the skills list for the skill-detail sidebar (the "data tables" style sidebar that appears
 * once the user opens an individual skill). Search state is local to the sidebar — it's intentionally
 * NOT wired to the global useAiSkillsStore.searchQuery so the main Skills list page and the sidebar
 * can be filtered independently.
 */
export default function useAiSkillsLeftSidebar(): UseAiSkillsLeftSidebarI {
    const [search, setSearch] = useState('');

    const {data, error, isLoading} = useAiSkillsQuery();

    const collator = useMemo(() => new Intl.Collator(undefined, {numeric: true, sensitivity: 'base'}), []);

    const filteredSkills = useMemo(() => {
        const query = search.trim().toLowerCase();
        const skills = [...(data?.aiSkills ?? [])].sort((skillA, skillB) =>
            collator.compare(skillA.name.trim(), skillB.name.trim())
        );

        if (!query) {
            return skills;
        }

        return skills.filter((skill) => skill.name.toLowerCase().includes(query));
    }, [data, search, collator]);

    return {
        error,
        filteredSkills,
        handleSearchChange: setSearch,
        isLoading,
        search,
    };
}
