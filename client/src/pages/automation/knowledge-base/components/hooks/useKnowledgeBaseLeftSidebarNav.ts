import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useKnowledgeBasesQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo, useState} from 'react';
import {useParams} from 'react-router-dom';

export default function useKnowledgeBaseLeftSidebarNav() {
    const [search, setSearch] = useState('');

    const {id} = useParams<{id: string}>();
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const workspaceId = String(currentWorkspaceId ?? 1049);

    const {data, isLoading} = useKnowledgeBasesQuery({
        environmentId: String(environmentId),
        workspaceId,
    });

    // Numeric + base sensitivity, like the data tables sidebar: "KB 10" sorts after "KB 9", and searching is
    // case- and accent-insensitive.
    const collator = useMemo(() => new Intl.Collator(undefined, {numeric: true, sensitivity: 'base'}), []);

    const knowledgeBases = useMemo(() => {
        const query = search.trim().toLowerCase();

        const sortedKnowledgeBases = (data?.knowledgeBases ?? [])
            .filter((knowledgeBase): knowledgeBase is NonNullable<typeof knowledgeBase> => knowledgeBase !== null)
            .sort((knowledgeBaseA, knowledgeBaseB) =>
                collator.compare(knowledgeBaseA.name.trim(), knowledgeBaseB.name.trim())
            );

        if (!query) {
            return sortedKnowledgeBases;
        }

        return sortedKnowledgeBases.filter((knowledgeBase) => knowledgeBase.name.toLowerCase().includes(query));
    }, [collator, data, search]);

    const handleSearchChange = (value: string) => {
        setSearch(value);
    };

    return {
        currentKnowledgeBaseId: id,
        handleSearchChange,
        isLoading,
        knowledgeBases,
        search,
    };
}
