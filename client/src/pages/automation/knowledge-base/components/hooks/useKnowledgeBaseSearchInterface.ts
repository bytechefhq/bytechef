import {useSearchKnowledgeBaseQuery} from '@/shared/middleware/graphql';
import {FormEvent, useState} from 'react';

interface UseKnowledgeBaseSearchInterfaceProps {
    knowledgeBaseId: string;
}

export default function useKnowledgeBaseSearchInterface({knowledgeBaseId}: UseKnowledgeBaseSearchInterfaceProps) {
    const [query, setQuery] = useState('');
    const [metadataFilters, setMetadataFilters] = useState('');
    const [searchQuery, setSearchQuery] = useState('');
    const [searchFilters, setSearchFilters] = useState<string | undefined>(undefined);

    const {data, isLoading} = useSearchKnowledgeBaseQuery(
        {
            id: knowledgeBaseId,
            metadataFilters: searchFilters,
            query: searchQuery,
        },
        {
            enabled: searchQuery.length > 0,
        }
    );

    const results = (data?.searchKnowledgeBase || []).filter(
        (result): result is NonNullable<typeof result> => result !== null
    );

    const handleSearch = (event: FormEvent) => {
        event.preventDefault();

        if (query.trim().length === 0) {
            return;
        }

        setSearchQuery(query);
        setSearchFilters(metadataFilters.trim() || undefined);
    };

    const handleClearSearch = () => {
        setQuery('');
        setMetadataFilters('');
        setSearchQuery('');
        setSearchFilters(undefined);
    };

    const canSearch = query.trim().length > 0;

    return {
        canSearch,
        handleClearSearch,
        handleSearch,
        isLoading,
        metadataFilters,
        query,
        results,
        searchQuery,
        setMetadataFilters,
        setQuery,
    };
}
